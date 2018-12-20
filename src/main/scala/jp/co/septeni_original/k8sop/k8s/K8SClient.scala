package jp.co.septeni_original.k8sop.k8s

import io.kubernetes.client.ApiClient
import io.kubernetes.client.util.Config
import jp.co.septeni_original.k8sop.util.GCPAuthenticator

import scala.util.Try

class K8SClient {

  private var client: ApiClient = getClient

  private def getClient: ApiClient = {
    Try(Config.defaultClient()).recoverWith {
      case _: IllegalStateException =>
        GCPAuthenticator.refresh.map(_ => Config.defaultClient())
    }
  }.get

  def refresh(): Unit = {
    GCPAuthenticator.refresh.map { _ =>
      this.client = getClient
    }
  }

  def underlying: ApiClient = client
}
