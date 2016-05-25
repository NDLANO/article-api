package no.ndla.contentapi.batch.service

import no.ndla.contentapi.model.ContentInformation
import org.jsoup.Jsoup
import org.jsoup.nodes.Entities.EscapeMode

trait ConverterServiceComponent {
    this: ConverterModules =>
  val converterService: ConverterService

  class ConverterService {
    def convertNode(node: ContentInformation): ContentInformation = {
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
