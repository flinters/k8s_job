package jp.co.septeni_original.k8sop.util

import java.util.TimerTask

import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration._
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future, Promise}

object FutureOps extends LazyLogging {

  private def timer = new java.util.Timer(true)

  def futureDelay[T](f: () => Future[T], delay: Duration): Future[T] = {
    val promise = Promise[T]()

    val timerTask = new TimerTask {
      override def run(): Unit = {
        promise.tryCompleteWith(f())
      }
    }

    timer.schedule(timerTask, delay.toMillis)
    promise.future
  }

  def retry[T](f: () => Future[T],
                   maxRetryCount: Int = 3,
                   delay: Duration = 0.second)(implicit ec: ExecutionContext): Future[T] = {
    f() recoverWith {
      case t if maxRetryCount > 0 => {
        logger.debug("function failed.", t)
        logger.debug(s"retry remain count: $maxRetryCount")
        val result = for {
          r <- futureDelay(() => retry(f, maxRetryCount - 1, delay), delay)
        } yield r
        Future(result).flatten
      }
      case t => Future.failed(t)
    }
  }

}
