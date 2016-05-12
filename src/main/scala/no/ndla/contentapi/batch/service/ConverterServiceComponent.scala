package no.ndla.contentapi.batch.service

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}


trait ConverterServiceComponent {
    this: ImportServiceComponent with ConverterModules =>
  val converterService: ConverterService

  class ConverterService {
    def convertNode(nodeId: String): String = {
      val node = importService.importNode(nodeId)
      convert(node.content)
    }

    def convert(htmlContent: String): String = {
      val document = Jsoup.parseBodyFragment(htmlContent).body().tagName("article")

      converterModules foreach {_.convert(document)}
      document.outerHtml()
    }
  }
}
