package jp.co.septeni_original.k8sop.util

import com.typesafe.scalalogging.LazyLogging

import scala.sys.process._
import scala.util.Try

object GCPTokenRefresher extends LazyLogging {

  private val voidPL: ProcessLogger = ProcessLogger(_ => ())

  def refresh(): Try[Unit] = Try {
    "kubectl get pods".!!(voidPL)
    ()
  }
}
