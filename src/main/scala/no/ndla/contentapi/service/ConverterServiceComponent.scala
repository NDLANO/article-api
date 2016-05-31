package no.ndla.contentapi.service

import no.ndla.contentapi.model.{ContentInformation, RequiredLibrary}
import org.jsoup.Jsoup
import org.jsoup.nodes.Entities.EscapeMode

import scala.collection.mutable.ListBuffer

trait ConverterServiceComponent {
    this: ConverterModules =>
  val converterService: ConverterService

  class ConverterService {
    def convertNode(node: ContentInformation): ContentInformation = {
      implicit val requiredLibraries = ListBuffer[RequiredLibrary]()
      val convertedContent = node.content.map(x => x.copy(content=convert(x.content)))
      node.copy(content=convertedContent, requiredLibraries=requiredLibraries.toList)
    }

    def convert(htmlContent: String)(implicit requiredLibraries: ListBuffer[RequiredLibrary]): String = {
      val document = Jsoup.parseBodyFragment(htmlContent)
      val firstElement = document.body().tagName("article")
      document.outputSettings().escapeMode(EscapeMode.xhtml)

      converterModules.foldLeft(firstElement)(
        (element, converter) => converter.convert(element)
      ).outerHtml()
    }
  }
}
