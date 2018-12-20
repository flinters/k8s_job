package jp.co.septeni_original.k8sop
import com.typesafe.scalalogging.LazyLogging
import io.digdag.spi.TaskResult
import io.kubernetes.client.models.V1Job
import io.kubernetes.client.util.Yaml
import jp.co.septeni_original.CreateJobOperator
import jp.co.septeni_original.k8sop.k8s.{ConfigMapClient, JobClient}

import scala.collection.JavaConverters._
import scala.concurrent.{Await, ExecutionContext, Future}

class K8STaskRunner(yamlString: String, config: K8SOperatorConfig)(implicit val ec: ExecutionContext)
    extends LazyLogging {

  def run(): Future[Unit] = {
    try {
      logger.info(s"${CreateJobOperator.JOB_NAME} start.")
      logger.debug(s"config: $config")

      val cm = new ConfigMapClient(client, config)
      val j  = new JobClient(client, config)

      val yamls = Yaml.loadAll(yamlString).asScala.toList ++ cmFromDir

      logger.info("resource create start.")

      for {
        _          <- cm.delete(yamls)
        _          <- cm.create(yamls)
        _          <- j.delete(yamls)
        job        <- j.create(yamls)
        jobResults <- j.wait(job.toList)
        _          <- cm.delete(yamls)
        _          <- j.delete(yamls)
      } yield {
        if (jobResults.forall(_.getStatus.getSucceeded > 0)) {
          ()
        } else {
          throw new RuntimeException("job failed.")
        }
      }
    }
  }

}
