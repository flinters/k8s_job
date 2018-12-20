package jp.co.septeni_original.k8sop.k8s

import com.squareup.okhttp.Response
import com.typesafe.scalalogging.LazyLogging
import io.kubernetes.client.apis.BatchV1Api
import io.kubernetes.client.models.{V1DeleteOptions, V1Job}
import io.kubernetes.client.{ApiClient, ApiException}
import jp.co.septeni_original.k8sop.K8SOperatorConfig
import jp.co.septeni_original.k8sop.util.FutureOps

import scala.concurrent.{ExecutionContext, Future}

class JobClient(val client: ApiClient, config: K8SOperatorConfig)(implicit ec: ExecutionContext) extends LazyLogging {
  private val api = new BatchV1Api(client)

  val defaultDeleteOptions = new V1DeleteOptions
  defaultDeleteOptions.setGracePeriodSeconds(0L)
  defaultDeleteOptions.setOrphanDependents(false)

  private def ols2jls(objects: List[Object]) =
    objects.filter(_.isInstanceOf[V1Job]).map(_.asInstanceOf[V1Job])

  def create(objects: List[Object]): Future[Seq[V1Job]] = {
    val jobsF =
      ols2jls(objects).map { j =>
        FutureOps.retry { () =>
          Future {
            logger.debug(s"Job create start. $j")
            val res = api.createNamespacedJob(j.getMetadata.getNamespace, j, "false")
            logger.debug(s"Job create complete.")
            res
          }
        }
      }

    Future.sequence(jobsF)
  }

  def wait(job: List[V1Job]): Future[Seq[V1Job]] = {
    val f = job.map { j =>
      FutureOps.retry { () =>
        Future {
          val job =
            api.readNamespacedJob(j.getMetadata.getName, j.getMetadata.getNamespace, "false", null, null)
          if (Option(job.getStatus).flatMap(j => Option(j.getConditions)).isEmpty) {
            throw new RuntimeException("job waiting")
          } else {
            job
          }
        }
      }
    }

    Future.sequence(f)
  }

  // V1Statusを返すメソッドが成功してしまうと "絶対に" IllegalStateExceptionが発生するのでcall経由で実行している。
  // 現時点でこの問題が治る見込みはないので、返却されたステータスのみを参照する。
  // https://github.com/kubernetes-client/java/issues/86
  def delete(objects: List[Object], deleteOption: V1DeleteOptions = defaultDeleteOptions): Future[Seq[Unit]] = {
    val jobF: Seq[Future[Unit]] = ols2jls(objects).map { j =>
      FutureOps.retry { () =>
        Future {
          logger.debug(s"Job delete start. $j")

          var res: Response = null
          try {
            res = api
              .deleteNamespacedJobCall(j.getMetadata.getName,
                                       j.getMetadata.getNamespace,
                                       deleteOption,
                                       "false",
                                       null,
                                       null,
                                       null,
                                       null,
                                       null)
              .execute()
          } catch {
            case ae: ApiException if ae.getCode == 404 => ()
            case th: Throwable                         => throw th
          } finally {
            Option(res).map(_.body).foreach(_.close)
          }
          logger.debug(s"Job delete complete.")
          ()
        }
      }
    }
    Future.sequence(jobF)
  }

}
