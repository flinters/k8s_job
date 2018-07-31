package jp.co.septeni_original.k8sop.util

import com.typesafe.scalalogging.LazyLogging
import io.kubernetes.client.ApiException
import retry.Success

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

object RetryOps extends LazyLogging {


  def retryWithRefresh[T](maxRetryCount: Int = 3, f: => Future[T])(
      implicit ec: ExecutionContext, success: Success[T]
  )= {
    retry.When {
      case ae: ApiException if ae.getCode != 404 => retry.JitterBackoff(maxRetryCount, 10.seconds)
      case ie: IllegalStateException if ie.getMessage == "Unimplemented" => {
        GCPAuthenticator.refresh
        retry.JitterBackoff(maxRetryCount, 10.seconds)
      }
    }.apply(f)
  }

}
