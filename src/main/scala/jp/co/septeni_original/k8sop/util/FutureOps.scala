package jp.co.septeni_original.k8sop.util

import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}

object FutureOps extends LazyLogging {

  def retryWith[T](f: => Future[T],
                   runOnRetry: Throwable => Future[_] = (t: Throwable) => Future.successful(()),
                   maxRetryCount: Int = 3)(implicit ec: ExecutionContext): Future[T] = {
    f recoverWith {
      case t if maxRetryCount > 0 => {
        logger.debug("function failed.", t)
        logger.debug(s"retry remain count: $maxRetryCount")
        val result = for {
          _ <- runOnRetry(t)
          r <- retryWith(f, runOnRetry, maxRetryCount - 1)
        } yield r
        Future(result).flatten
      }
      case t => Future.failed(t)
    }
  }

  def retryWithRefresh[T](f: => T, maxRetryCount: Int = 3)(implicit ec: ExecutionContext): Future[T] = {
    def refresh(t: Throwable) = t match {
      case _: IllegalStateException => Future.fromTry(GCPAuthenticator.refresh)
      case _                        => Future.successful(())
    }
    retryWith(Future(f), refresh, maxRetryCount)
  }

}
