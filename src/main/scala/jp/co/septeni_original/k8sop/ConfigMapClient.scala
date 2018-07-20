package jp.co.septeni_original.k8sop

import com.typesafe.scalalogging.LazyLogging
import io.kubernetes.client.apis.CoreV1Api
import io.kubernetes.client.models.{V1ConfigMap, V1DeleteOptions}
import io.kubernetes.client.util.Yaml
import io.kubernetes.client.{ApiClient, ApiException}
import jp.co.septeni_original.k8sop.util.FutureOps

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

class ConfigMapClient(val client: ApiClient)(implicit ec: ExecutionContext) extends LazyLogging {
  private val api = new CoreV1Api(client)

  val defaultDeleteOptions = new V1DeleteOptions
  defaultDeleteOptions.setGracePeriodSeconds(0L)

  private def ols2cmls(objects: List[Object]) =
    objects.filter(_.isInstanceOf[V1ConfigMap]).map(_.asInstanceOf[V1ConfigMap]) // collect

  def createFrom(map: Map[String, String]): V1ConfigMap = {
    val baseYaml = map.getOrElse("base.yaml", throw new IllegalArgumentException("base.yaml not found"))

    val baseCM = Yaml.loadAs(baseYaml, classOf[V1ConfigMap])
    logger.debug(s"base ConfigMap: $baseCM")

    val newData: Map[String, String] = Option(baseCM.getData)
      .map(_.asScala.toMap)
      .getOrElse(Map[String, String]()) ++ map.filter(_._1 != "base.yaml")
    baseCM.setData(newData.asJava)
    baseCM
  }

  def create(objects: List[Object]): Future[Seq[V1ConfigMap]] = {
    val cmF = ols2cmls(objects).map { (cm: V1ConfigMap) =>
      logger.debug(s"ConfigMap create start. cm: $cm")
      FutureOps.retryWithRefresh {
        Future {
          val res = api.createNamespacedConfigMap(cm.getMetadata.getNamespace, cm, "false")
          logger.debug(s"ConfigMap create complete.")
          res
        }
      }
    }
    Future.sequence(cmF)
  }

  // V1Statusを返すメソッドが成功してしまうと "絶対に" IllegalStateExceptionが発生するのでcall経由で実行している。
  // 現時点でこの問題が治る見込みはないので、返却されたステータスのみを参照する。
  // https://github.com/kubernetes-client/java/issues/86
  def delete(objects: List[Object], deleteOption: V1DeleteOptions = defaultDeleteOptions): Future[Seq[Unit]] = {
    val cmF = ols2cmls(objects).map { cm =>
      logger.debug(s"ConfigMap delete start. cm: $cm")
      FutureOps.retryWithRefresh {
        Future {
          api
            .deleteNamespacedConfigMapCall(cm.getMetadata.getName,
                                           cm.getMetadata.getNamespace,
                                           deleteOption,
                                           "false",
                                           null,
                                           null,
                                           null,
                                           null,
                                           null)
            .execute()
          logger.debug(s"ConfigMap delete complete.")
          ()
        } recover {
          case notFound: ApiException if notFound.getCode == 404 => ()
        }
      }
    }
    Future.sequence(cmF)
  }
}
