package jp.co.septeni_original.k8sop

import com.squareup.okhttp.Response
import io.kubernetes.client.ApiException
import io.kubernetes.client.apis.{BatchV1Api, CoreV1Api}
import io.kubernetes.client.models.{V1ConfigMap, V1DeleteOptions, V1Job}
import io.kubernetes.client.util.authenticators.GCPAuthenticator
import io.kubernetes.client.util.{Config, KubeConfig}
import jp.co.septeni_original.k8sop.util.{GCPTokenRefresher, TryOps}

import scala.util.Try

object K8sApiClient {

  GCPTokenRefresher.refresh()
  KubeConfig.registerAuthenticator(new GCPAuthenticator)

  var client = Config.defaultClient

  private var batch = new BatchV1Api(client)
  private var core  = new CoreV1Api(client)

  val defaultDeleteOptions = new V1DeleteOptions
  defaultDeleteOptions.setGracePeriodSeconds(0L)
  defaultDeleteOptions.setOrphanDependents(false)

  def jobsFrom(objects: List[Object]) =
    objects.collect{ case job:V1Job => job}

  def configMapFrom(objects: List[Object]) =
    objects.collect{ case cm:V1ConfigMap => cm}

  def createJob(jobs: List[V1Job]): Try[List[V1Job]] = retryWithTokenRefresh {
    jobs.map { job =>
      batch.createNamespacedJob(job.getMetadata.getNamespace, job, false, "false", null)
    }
  }

  /**
    * @param jobs
    * @return if all job completed, then returns true.
    */
  def isCompleted(jobs: List[V1Job]): Try[Boolean] = retryWithTokenRefresh {
    jobs
      .map { job =>
        val j = batch.readNamespacedJob(job.getMetadata.getName, job.getMetadata.getNamespace, "false", null, null)
        Option(j.getStatus).flatMap(jo => Option(jo.getConditions)).isDefined
      }
      .forall(_ == true)
  }

  // V1Statusを返すメソッドが成功してしまうと "絶対に" IllegalStateExceptionが発生するのでcall経由で実行している。
  // 現時点でこの問題が治る見込みはないので、返却されたステータスのみを参照する。
  // https://github.com/kubernetes-client/java/issues/86
  def deleteJob(jobs: List[V1Job]): Try[Unit] = retryWithTokenRefresh {
    val jobF: List[Try[Unit]] = jobs.map { j =>
      var res: Response = null
      try {
        res = batch.deleteNamespacedJob(j.getMetadata.getName,
                                        j.getMetadata.getNamespace,
                                        defaultDeleteOptions,
                                        "false",
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null)
      } catch {
        case ae: ApiException if ae.getCode == 404 => ()
        case th: Throwable                         => throw th
      } finally {
        Option(res).map(_.body).foreach(_.close)
      }
      ()
    }
    TryOps.sequence(jobF)
  }

  def retryWithTokenRefresh[T](f: => T): Try[T] =
    Try(f).recover {
      case IllegalStateException =>
        GCPTokenRefresher.refresh()
        this.client = Config.defaultClient
        this.batch = new BatchV1Api(this.client)
        this.core = new CoreV1Api(this.client)

        f
    }


  def createConfigMap(configMap: List[V1ConfigMap]): Try[Unit] =
}
