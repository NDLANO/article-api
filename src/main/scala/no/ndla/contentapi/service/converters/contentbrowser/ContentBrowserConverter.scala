package no.ndla.contentapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.integration.{ConverterModule, LanguageContent}
import no.ndla.contentapi.model.{Content, ImportStatus, RequiredLibrary}
import no.ndla.contentapi.service.ExtractServiceComponent
import no.ndla.contentapi.service.converters.SimpleTagConverter._
import org.jsoup.nodes.Element

trait ContentBrowserConverter {
  this: ExtractServiceComponent with ImageConverterModule with LenkeConverterModule with H5PConverterModule with AktualitetConverterModule =>
  val contentBrowserConverter: ContentBrowserConverter

  class ContentBrowserConverter extends ConverterModule with LazyLogging {
    private val contentBrowserModules = Map[String, ContentBrowserConverterModule](
      ImageConverter.typeName -> ImageConverter,
      H5PConverter.typeName -> H5PConverter,
      LenkeConverter.typeName -> LenkeConverter,
      AktualitetConverter.typeName -> AktualitetConverter)

    def convert(content: LanguageContent): (LanguageContent, ImportStatus) = {
      val element = stringToJsoupDocument(content.content)
      var isContentBrowserField = false
      var requiredLibraries = List[RequiredLibrary]()
      var importStatus = ImportStatus()

      do {
        val text = element.html()
        val cont = ContentBrowser(text)

        isContentBrowserField = cont.isContentBrowserField()
        if (isContentBrowserField) {
          val nodeType = extractService.getNodeType(cont.get("nid")).getOrElse("UNKNOWN")

          val (newContent, reqLibs, messages) = contentBrowserModules.get(nodeType) match {
            case Some(module) => module.convert(cont)
            case None => {
              val errorString = s"{Unsupported content ${nodeType}: ${cont.get("nid")}}"
              logger.warn(errorString)
              (errorString, List[RequiredLibrary](), List(errorString))
            }
          }
          requiredLibraries = requiredLibraries ::: reqLibs
          importStatus = ImportStatus(importStatus.messages ++ messages)

          val (start, end) = cont.getStartEndIndex()
          element.html(text.substring(0, start) + newContent + text.substring(end))
        }
      } while (isContentBrowserField)

      (content.copy(content=jsoupDocumentToString(element), requiredLibraries=requiredLibraries), importStatus)
    }
  }
}
