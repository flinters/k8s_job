package jp.co.septeni_original.k8sop

import java.nio.charset.StandardCharsets.UTF_8

import com.typesafe.scalalogging.LazyLogging
import io.digdag.spi._
import io.digdag.util.BaseOperator
import io.kubernetes.client.Configuration
import io.kubernetes.client.models._
import io.kubernetes.client.util.authenticators.GCPAuthenticator
import io.kubernetes.client.util.{Config, KubeConfig, Yaml}
import jp.co.septeni_original.k8sop.util.{FileReader, GCPTokenRefresher}

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

class CreateJobOperatorFactory(val templateEngine: TemplateEngine) extends OperatorFactory {
  override def getType: String = CreateJobOperator.JOB_NAME

  override def newOperator(context: OperatorContext) =
    new CreateJobOperator(context, templateEngine)
}

object CreateJobOperator {
  val JOB_NAME = "k8s_job"
}

private[k8sop] class CreateJobOperator private[k8sop] (val _context: OperatorContext,
                                                       val templateEngine: TemplateEngine)
    extends BaseOperator(_context)
    with LazyLogging {

  override def runTask: TaskResult = {
    logger.info(s"${CreateJobOperator.JOB_NAME} start.")


    val config = request.getConfig.mergeDefault(request.getConfig.getNestedOrGetEmpty(CreateJobOperator.JOB_NAME))

    logger.debug(s"config: $config")

    val templateYaml = workspace.templateCommand(templateEngine, config, CreateJobOperator.JOB_NAME, UTF_8)
    val cmDirNames   = config.getListOrEmpty("cmdir", classOf[String]).asScala.toList

    val cm = new ConfigMapClient()
    val j  = new JobClient()

    logger.info(s"config map loading from directoryes. $cmDirNames")
    val cmFromDir = cmDirNames
      .map(workspace.getPath)
      .map(_.toFile)
      .map(FileReader.directoryToMap)
      .map {
        _.mapValues(v => templateEngine.template(v, config))
      }
      .map(cm.createFrom)
    val yamls = Yaml.loadAll(templateYaml).asScala.toList ++ cmFromDir

    logger.info("resource create start.")

    val f: Try[Seq[V1Job]] = for {
      _          <- cm.delete(yamls)
      _          <- cm.create(yamls)
      _          <- j.delete(yamls)
      job        <- j.create(yamls)
      jobResults <- j.wait(job.toList)
      _          <- cm.delete(yamls)
      _          <- j.delete(yamls)
    } yield jobResults

    f match {
      case Success(s) if s.seq.forall(_.getStatus.getSucceeded > 0) =>
        TaskResult.empty(request)
      case Failure(e) => throw new TaskExecutionException(e)
    }
  }

}
