package jp.co.septeni_original.k8sop

import java.nio.file.Path

import io.digdag.client.config.Config
import jp.co.septeni_original.k8sop.K8SOperatorConfigKey.{ConfigMapDirectoryKey, PodDeletePolicyKey, TimeoutKey}
import jp.co.septeni_original.k8sop.PodDeletePolicy.Always
import jp.co.septeni_original.k8sop.util.FileReader

import scala.collection.JavaConverters._
import scala.concurrent.duration.{Duration, _}
import scala.util.Try

sealed abstract class K8SOperatorConfigKey(val key :String)

object K8SOperatorConfigKey {

  case object TimeoutKey extends K8SOperatorConfigKey("timeout")
  case object ConfigMapDirectoryKey extends K8SOperatorConfigKey("cmdir")
  case object PodDeletePolicyKey extends K8SOperatorConfigKey("delete_policy")

}

sealed abstract class PodDeletePolicy(val value: String)

object PodDeletePolicy {

  sealed object Always extends PodDeletePolicy("always")
  sealed object Never extends PodDeletePolicy("never")
  sealed object Succeed extends PodDeletePolicy("succeed")

  def valueOf(value: String): PodDeletePolicy = value match {
    case Always.value => Always
    case Never.value => Never
    case Succeed.value => Succeed
  }
}

case class K8SOperatorConfig(workspace: Path,timeout: Duration, configMapDirectory: List[String], podDeletePolicy: PodDeletePolicy) {

  def templateFiles: List[Path] = {
    configMapDirectory
      .map(workspace.resolve)
      .map(_.toFile)
      .map(FileReader.directoryToMap)
      .map {
        _.mapValues(v => templateEngine.template(v, config))
      }
      .map(cm.createFrom)
  }

}

object K8SOperatorConfig {

  def apply(workspace: Path, config: Config) =
  K8SOperatorConfig (
    workspace,
    Try(config.get(TimeoutKey.key, classOf[Long])).toOption.map(_.seconds).getOrElse(1.hour),
    config.getListOrEmpty(ConfigMapDirectoryKey.key, classOf[String]).asScala.toList,
    Try(config.get(PodDeletePolicyKey.key, classOf[String])).toOption.map(PodDeletePolicy.valueOf).getOrElse(Always)
  )

}
