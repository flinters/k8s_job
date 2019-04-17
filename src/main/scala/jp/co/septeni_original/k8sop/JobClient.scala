package jp.co.septeni_original.k8sop

import com.typesafe.scalalogging.LazyLogging
import io.kubernetes.client.models.{V1DeleteOptions, V1Job}
import jp.co.septeni_original.k8sop.util.TryOps

import scala.util.{Success, Try}

class JobClient extends LazyLogging {
  val defaultDeleteOptions = new V1DeleteOptions
  defaultDeleteOptions.setGracePeriodSeconds(0L)
  defaultDeleteOptions.setOrphanDependents(false)

  private def ols2jls(objects: List[Object]) =
    objects.filter(_.isInstanceOf[V1Job]).map(_.asInstanceOf[V1Job])

  def create(objects: List[Object]): Try[List[V1Job]] = {
    for {
      jobs <- Success(K8sApiClient.jobsFrom(objects))
      createdJobs <- K8sApiClient.createJob(jobs)
    } yield createdJobs
  }

  def wait(jobs: List[V1Job]): Try[Unit] = {
    K8sApiClient.isCompleted(jobs).flatMap {
      if(_) {
        Success(())
      } else {
        Thread.sleep(2000)
        wait(jobs)
      }
    }
  }

  def delete(objects: List[Object], deleteOption: V1DeleteOptions = defaultDeleteOptions): Try[Unit] = {
    TryOps.retry(K8sApiClient.deleteJob(ols2jls(objects))).flatten
  }

}
