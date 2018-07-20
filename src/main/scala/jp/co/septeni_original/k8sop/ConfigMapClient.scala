package jp.co.septeni_original.k8sop

import java.io.File
import java.nio.file.Paths

import com.nimbusds.jose.util.StandardCharset
import com.typesafe.scalalogging.LazyLogging
import io.kubernetes.client.apis.CoreV1Api
import io.kubernetes.client.models.{V1ConfigMap, V1DeleteOptions}
import io.kubernetes.client.util.Yaml
import io.kubernetes.client.{ApiClient, ApiException}
import jp.co.septeni_original.k8sop.util.FutureOps
import org.apache.commons.io.FileUtils

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

class ConfigMapClient(val client: ApiClient)(implicit ec: ExecutionContext) extends LazyLogging {
  private val api = new CoreV1Api(client)

  val defaultDeleteOptions = new V1DeleteOptions
  defaultDeleteOptions.setGracePeriodSeconds(0L)

  private def ols2cmls(objects: List[Object]) =
    objects.filter(_.isInstanceOf[V1ConfigMap]).map(_.asInstanceOf[V1ConfigMap]) // collect

  def createFrom(directoryName: String) = {
    val directory = Paths.get(directoryName).toFile
    require(directory.isDirectory, s"$directory isn't Directory.")
    logger.debug(s"directory found. ${directory.getAbsolutePath}")

    val files: List[File] = directory.listFiles().toList
    val baseYaml =
      files.find(_.getName == "base.yaml").getOrElse(throw new IllegalArgumentException("base.yaml not found"))

    val baseCM = Yaml.loadAs(baseYaml, classOf[V1ConfigMap])
    logger.debug(s"base ConfigMap: $baseCM")

    val dataFromFiles: Map[String, String] = files
      .filter(_.getName != "base.yaml")
      .filter(_.isFile)
      .map { f =>
        (f.getName, FileUtils.readFileToString(f, StandardCharset.UTF_8))
      }
      .toMap

    val newData: Map[String, String] = Option(baseCM.getData).map(_.asScala.toMap)
      .getOrElse(Map[String, String]()) ++ dataFromFiles
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
