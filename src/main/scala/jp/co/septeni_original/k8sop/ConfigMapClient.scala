package jp.co.septeni_original.k8sop

import com.squareup.okhttp.Response
import com.typesafe.scalalogging.LazyLogging
import io.kubernetes.client.ApiException
import io.kubernetes.client.models.{V1ConfigMap, V1DeleteOptions}
import io.kubernetes.client.util.Yaml
import jp.co.septeni_original.k8sop.util.TryOps

import scala.collection.JavaConverters._
import scala.util.Try

class ConfigMapClient extends LazyLogging {
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

  def create(objects: List[Object]): Try[Seq[V1ConfigMap]] = {
    val cmF = ols2cmls(objects).map { cm =>
      logger.debug(s"ConfigMap create start. cm: ${cm.getMetadata.getName}")
      Try {
        val res = api.createNamespacedConfigMap(cm.getMetadata.getNamespace, cm, false, "false", null)
        logger.debug(s"ConfigMap create complete.")
        res
      }
    }
    TryOps.sequence(cmF)
  }

  // V1Statusを返すメソッドが成功してしまうと "絶対に" IllegalStateExceptionが発生するのでcall経由で実行している。
  // 現時点でこの問題が治る見込みはないので、返却されたステータスのみを参照する。
  // https://github.com/kubernetes-client/java/issues/86
  def delete(objects: List[Object], deleteOption: V1DeleteOptions = defaultDeleteOptions): Try[Seq[Unit]] = {
    val cmF = ols2cmls(objects).map { cm =>
      logger.debug(s"ConfigMap delete start. cm: ${cm.getMetadata.getName}")
      TryOps.retry {
        var res: Response = null
        try {
          res = api
            .deleteNamespacedConfigMapCall(cm.getMetadata.getName,
                                           cm.getMetadata.getNamespace,
                                           deleteOption,
                                           "false",
                                           null,
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
      }
    }
    TryOps.sequence(cmF)
  }
}
