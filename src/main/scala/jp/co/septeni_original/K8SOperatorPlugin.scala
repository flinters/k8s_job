package jp.co.septeni_original

import java.util
import java.util.{List => JList}

import io.digdag.spi.{OperatorFactory, OperatorProvider, Plugin, TemplateEngine}
import javax.inject.Inject
import jp.co.septeni_original.k8sop.CreateJobOperatorFactory

object K8SOperatorPlugin {

  class K8SOperatorProvider @Inject()(templateEngine: TemplateEngine) extends OperatorProvider {

    override def get: JList[OperatorFactory] = util.Arrays.asList(new CreateJobOperatorFactory(templateEngine))
  }

}

class K8SOperatorPlugin extends Plugin {
  override def getServiceProvider[T](`type`: Class[T]): Class[_ <: T] = {
    if (`type` eq classOf[OperatorProvider]) classOf[K8SOperatorPlugin.K8SOperatorProvider].asSubclass(`type`)
    else null
  }
}
