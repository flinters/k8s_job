package jp.co.septeni_original.k8sop

import com.squareup.okhttp.Response
import com.typesafe.scalalogging.LazyLogging
import io.kubernetes.client.ApiClient
import io.kubernetes.client.apis.BatchV1Api
import io.kubernetes.client.models.{V1DeleteOptions, V1Job}
import jp.co.septeni_original.k8sop.util.RetryOps
import retry.Success

import scala.concurrent.{ExecutionContext, Future}

class JobClient(val client: ApiClient)(implicit ec: ExecutionContext) extends LazyLogging {
  private val api = new BatchV1Api(client)

  implicit def jobCompelte = Success[V1Job] { job =>
    Option(job.getStatus).flatMap(j => Option(j.getConditions)).isEmpty
  }

  implicit def jobDeleteCompelte = Success[Unit](_ => true)

  val defaultDeleteOptions = new V1DeleteOptions
  defaultDeleteOptions.setGracePeriodSeconds(0L)
  defaultDeleteOptions.setOrphanDependents(false)

  private def ols2jls(objects: List[Object]) =
    objects.filter(_.isInstanceOf[V1Job]).map(_.asInstanceOf[V1Job])

  def create(objects: List[Object]): Future[Seq[V1Job]] =
    Future.sequence {
      ols2jls(objects).map { j =>
        RetryOps
          .retryWithRefresh(
            3,
            Future(api.createNamespacedJob(j.getMetadata.getNamespace, j, "false"))
          )
      }
    }

  def wait(job: List[V1Job]): Future[Seq[V1Job]] = {
    val f = job.map { j =>
      RetryOps
        .retryWithRefresh(
          3,
          Future(
            api
              .readNamespacedJob(j.getMetadata.getName, j.getMetadata.getNamespace, "false", null, null)
          )
        )
    }

    Future.sequence(f)
  }

  // V1Statusを返すメソッドが成功してしまうと "絶対に" IllegalStateExceptionが発生するのでcall経由で実行している。
  // 現時点でこの問題が治る見込みはないので、返却されたステータスのみを参照する。
  // https://github.com/kubernetes-client/java/issues/86
  def delete(objects: List[Object], deleteOption: V1DeleteOptions = defaultDeleteOptions): Future[Seq[Unit]] = {
    val jobF: Seq[Future[Unit]] = ols2jls(objects).map { j =>
      RetryOps
        .retryWithRefresh(
          3,
          Future {
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
            } finally {
              Option(res).flatMap(r => Option(r.body)).foreach(_.close)
            }
            ()
          }
        )
    }
    Future.sequence(jobF)
  }

}
