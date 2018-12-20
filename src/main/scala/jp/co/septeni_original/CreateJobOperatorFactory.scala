package jp.co.septeni_original

import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.Executors

import com.typesafe.scalalogging.LazyLogging
import io.digdag.client.config.Config
import io.digdag.spi._
import io.digdag.util.BaseOperator
import jp.co.septeni_original.k8sop.{K8SOperatorConfig, K8STaskRunner}

import scala.concurrent.{Await, ExecutionContext}
import scala.util.control.NonFatal

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

  override def runTask: TaskResult =
    try {
      val es                            = Executors.newSingleThreadExecutor()
      implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(es)

      try {

        val rawConfig: Config =
          request.getConfig.mergeDefault(request.getConfig.getNestedOrGetEmpty(CreateJobOperator.JOB_NAME))
        val templateYaml = workspace.templateCommand(templateEngine, rawConfig, CreateJobOperator.JOB_NAME, UTF_8)
        val config       = K8SOperatorConfig(workspace.getPath, config)

        val runner = new K8STaskRunner(templateYaml, config)

        Await.result(runner.run(), config.timeout)
        TaskResult.builder().build()

      } finally {
        logger.info(s"${CreateJobOperator.JOB_NAME} complete. job count : ${job.map(_.length)}")
        es.shutdown()
      }

    } catch {
      case NonFatal(e) =>
        logger.error("unexpected error occurred.", e)
        throw new TaskExecutionException(e)
    }

}
