package jp.co.septeni_original.k8sop

import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.Executors

import com.typesafe.scalalogging.LazyLogging
import io.digdag.spi._
import io.digdag.util.BaseOperator
import io.kubernetes.client.Configuration
import io.kubernetes.client.models._
import io.kubernetes.client.util.authenticators.GCPAuthenticator
import io.kubernetes.client.util.{Config, KubeConfig, Yaml}
import jp.co.septeni_original.k8sop.util.{FileReader, FutureOps}

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

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

  val es                            = Executors.newCachedThreadPool
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(es)

  override def runTask: TaskResult = {
    logger.info(s"${CreateJobOperator.JOB_NAME} start.")

    KubeConfig.registerAuthenticator(new GCPAuthenticator)
    val clientF = FutureOps.retryWithRefresh(Config.defaultClient)
    val client  = Await.result(clientF, Duration.Inf)
    Configuration.setDefaultApiClient(client)

    val config = request.getConfig.mergeDefault(request.getConfig.getNestedOrGetEmpty(CreateJobOperator.JOB_NAME))

    logger.debug(s"config: $config")

    val templateYaml = workspace.templateCommand(templateEngine, config, CreateJobOperator.JOB_NAME, UTF_8)
    val timeout      = config.get("timeout", classOf[Int])
    val cmDirNames   = config.getListOrEmpty("cmdir", classOf[String]).asScala.toList

    val cm = new ConfigMapClient(client)
    val j  = new JobClient(client)

    logger.info(s"comfig map loading from directoryes. $cmDirNames")
    val cmFromDir = cmDirNames
      .map(workspace.getPath)
      .map(_.toFile)
      .map(FileReader.directoryToMap)
      .map { _.mapValues(v => templateEngine.template(v, config)) }
      .map(cm.createFrom)
    val yamls = Yaml.loadAll(templateYaml).asScala.toList ++ cmFromDir

    logger.info("resource create start.")

    val f: Future[Seq[V1Job]] = for {
      _          <- cm.delete(yamls)
      _          <- cm.create(yamls)
      _          <- j.delete(yamls)
      job        <- j.create(yamls)
      jobResults <- j.wait(job.toList)
      _          <- cm.delete(yamls)
      _          <- j.delete(yamls)
    } yield jobResults

    f onComplete { job =>
      es.shutdown()
      logger.info(s"${CreateJobOperator.JOB_NAME} complete. job count : ${job.map(_.length)}")
    }
    val results: Seq[V1Job] = Await.result(f, timeout.seconds)

    if (results.forall(_.getStatus.getSucceeded > 0)) {
      TaskResult.empty(request)
    } else {
      logger.error(results.toString())
      throw new RuntimeException("job failed.")
    }
  }

}
