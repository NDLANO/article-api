package no.ndla.contentapi.service

import no.ndla.contentapi.model.{ContentInformation, ImportErrors, RequiredLibrary}
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities.EscapeMode

trait ConverterServiceComponent {
  this: ConverterModules =>
  val converterService: ConverterService

  class ConverterService {
    def convertNode(node: ContentInformation): (ContentInformation, ImportErrors) = {
      var requiredLibraries = List[RequiredLibrary]()
      var errorList = List[String]()

      val convertedContent = node.content.map(x => {
        val (content, libs, errors) = convert(x.content)
        requiredLibraries = requiredLibraries ::: libs
        errorList = errorList ::: errors
        x.copy(content=content.outerHtml())
      })
      (node.copy(content=convertedContent, requiredLibraries=requiredLibraries.distinct), ImportErrors(errorList))
    }

    def convert(htmlContent: String): (Element, List[RequiredLibrary], List[String]) = {
      val document = Jsoup.parseBodyFragment(htmlContent)
      val firstElement = document.body().tagName("article")
      document.outputSettings().escapeMode(EscapeMode.xhtml)

      converterModules.foldLeft((firstElement, List[RequiredLibrary](), List[String]()))(
        (element, converter) => {
          val (el, libs, errorList) = element
          val (convertedEl, newRequiredLibs, newErrors) = converter.convert(el)
          (convertedEl, libs ::: newRequiredLibs, errorList ::: newErrors)
        }
      )
    }
  }
}
