package no.ndla.contentapi.batch.service

import no.ndla.contentapi.model.ContentInformation
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities.EscapeMode

trait ConverterServiceComponent {
    this: ImportServiceComponent with ConverterModules =>
  val converterService: ConverterService

  class ConverterService {
    def convertNode(nodeId: String): ContentInformation = {
      val node = importService.importNode(nodeId)
      val convertedContent = node.content.map(x => x.copy(content=convert(x.content)))
      node.copy(content=convertedContent)
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
