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
      val firstElement = document.body().tagName("article")
      document.outputSettings().escapeMode(EscapeMode.xhtml)

      converterModules.foldLeft(firstElement)(
        (element, converter) => converter.convert(element)
      ).outerHtml()
    }
  }
}
