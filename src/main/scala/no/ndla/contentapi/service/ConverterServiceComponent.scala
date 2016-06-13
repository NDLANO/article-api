package no.ndla.contentapi.service

import no.ndla.contentapi.ContentApiProperties
import no.ndla.contentapi.model.{Content, ContentInformation, ImportErrors, RequiredLibrary}
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
        val (content, libs, errors) = convert(x, ContentApiProperties.maxConvertionPasses)
        requiredLibraries = requiredLibraries ::: libs
        errorList = errorList ::: errors
        x.copy(content=content.outerHtml())
      })
      (node.copy(content=convertedContent, requiredLibraries=requiredLibraries.distinct), ImportErrors(errorList))
    }

    def convert(content: Content, maxPasses: Int): (Element, List[RequiredLibrary], List[String]) = {
      val document = Jsoup.parseBodyFragment(content.content)
      val firstElement = document.body().tagName("article")
      document.outputSettings().escapeMode(EscapeMode.xhtml)

      convertWhileUnfinished(firstElement, content.language.getOrElse(""), maxPasses, List[RequiredLibrary](), List[String]())
    }

    private def convertWhileUnfinished(el: Element, language: String, maxPassesLeft: Int, requiredLibraries: List[RequiredLibrary], errors: List[String]): (Element, List[RequiredLibrary], List[String]) = {
      val originalContents = el.outerHtml()
      val (newElement, reqLibs, errorMsgs) = converterModules.foldLeft((el, List[RequiredLibrary](), List[String]()))(
        (element, converter) => {
          val (el, libs, errorList) = element
          val (convertedEl, newRequiredLibs, newErrors) = converter.convert(el, language)
          (convertedEl, libs ::: newRequiredLibs, errorList ::: newErrors)
        }
      )

      el.outerHtml() == originalContents || maxPassesLeft == 0 match {
        case true => (newElement, requiredLibraries ::: reqLibs, errors ::: errorMsgs)
        case false => convertWhileUnfinished(newElement, language, maxPassesLeft - 1, requiredLibraries ::: reqLibs, errors ::: errorMsgs)
      }
    }
  }
}
