package jp.co.septeni_original.k8sop.util

import java.util.concurrent.atomic.AtomicLong

import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration._
import scala.sys.process._
import scala.util.Try

// 以下の問題が解決されると不要になるが、その時まではkubectlを通して解決する.
// https://github.com/kubernetes-client/java/issues/290
object GCPAuthenticator extends LazyLogging {

  private val lastRefreshedAt = new AtomicLong(0)

  // トークンの有効期限切れは同時に多数発生する可能性が高いので、synchronizedで直列化し、最終リフレッシュ時間を取得することで何度もkubectl叩くのを防ぐ
  // 30秒に深い意味はない
  def refresh: Try[Unit] = Try {
    synchronized {
      val now = System.currentTimeMillis()
      if ( (now - 30.seconds.toMillis) > lastRefreshedAt.get()) {
        logger.debug(s"token refresh. last refreshed at : $lastRefreshedAt")
        "kubectl get pods".!!
        lastRefreshedAt.set(now)
        logger.debug(s"token refreshed. last refreshed at : $lastRefreshedAt")
      }
    }
  }

}
