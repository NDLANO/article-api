package no.ndla.contentapi.batch.service

import no.ndla.contentapi.model.ContentInformation
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

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
      var element = Jsoup.parseBodyFragment(htmlContent).body().tagName("article")
      for (module <- converterModules)
        element = module.convert(element)
      element.outerHtml()
    }
  }
}
