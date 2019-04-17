package jp.co.septeni_original.k8sop.util

import com.typesafe.scalalogging.LazyLogging

import scala.util.{Failure, Try}

object TryOps extends LazyLogging {

  def retry[T](f: => T, maxRetryCount: Int = 3): Try[T] = {
    Try(f) recoverWith {
      case _ if maxRetryCount > 0 => retry(f, maxRetryCount - 1)
      case t                      => Failure(t)
    }
  }


  def sequence[T](ls: List[Try[T]]): Try[List[T]] = Try(ls.map(_.get))
}
