package no.ndla.contentapi.batch.service

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

trait ConverterServiceComponent {
    this: ImportServiceComponent with ConverterModules =>
  val converterService: ConverterService

  class ConverterService {
    def convertNode(nodeId: String): String = {
      val node = importService.importNode(nodeId)
      convert(node.content)
    }

    def convert(htmlContent: String): String = {
      var element = Jsoup.parseBodyFragment(htmlContent).body().tagName("article")
      for (module <- converterModules)
        element = module.convert(element)
      element.outerHtml()
    }
  }
}
