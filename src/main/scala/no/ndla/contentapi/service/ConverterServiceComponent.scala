package no.ndla.contentapi.service

import no.ndla.contentapi.model.{ContentInformation, RequiredLibrary}
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities.EscapeMode

import scala.collection.mutable.ListBuffer

case class ImportErrors(errors: List[String])

trait ConverterServiceComponent {
    this: ConverterModules =>
  val converterService: ConverterService

  class ConverterService {
    def convertNode(node: ContentInformation): (ContentInformation, ImportErrors) = {
      val requiredLibraries = ListBuffer[RequiredLibrary]()
      val errorList = ListBuffer[String]()

      val convertedContent = node.content.map(x => x.copy(content=convert(x.content, requiredLibraries, errorList)))
      (node.copy(content=convertedContent, requiredLibraries=requiredLibraries.toList), ImportErrors(errorList.toList))
    }

    def convert(htmlContent: String, requiredLibraries: ListBuffer[RequiredLibrary], errorList: ListBuffer[String]): String = {
      val document = Jsoup.parseBodyFragment(htmlContent)
      val firstElement = document.body().tagName("article")
      document.outputSettings().escapeMode(EscapeMode.xhtml)

      converterModules.foldLeft(firstElement)(
        (element, converter) => converter.convert(element, requiredLibraries, errorList)
      ).outerHtml()
    }
  }
}
