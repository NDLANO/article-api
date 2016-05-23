package no.ndla.contentapi.batch.service

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities.EscapeMode

trait ConverterServiceComponent {
    this: ImportServiceComponent with ConverterModules =>
  val converterService: ConverterService

  class ConverterService {
    def convertNode(nodeId: String): String = {
      val node = importService.importNode(nodeId)
      convert(node.content)
    }

    def convert(htmlContent: String) = {
      val document = Jsoup.parseBodyFragment(htmlContent)
      var element = document.body().tagName("article")

      document.outputSettings().escapeMode(EscapeMode.xhtml)

      for (module <- converterModules)
        element = module.convert(element)
      element.outerHtml()
    }
  }
}
