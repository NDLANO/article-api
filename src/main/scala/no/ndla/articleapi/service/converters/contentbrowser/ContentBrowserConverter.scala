package no.ndla.articleapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.integration.{ConverterModule, LanguageContent}
import no.ndla.articleapi.model.{ImportStatus, RequiredLibrary}
import org.jsoup.nodes.Element

import scala.annotation.tailrec

trait ContentBrowserConverter {
  this: ContentBrowserConverterModules =>
  val contentBrowserConverter: ContentBrowserConverter

  class ContentBrowserConverter extends ConverterModule with LazyLogging {
    private val contentBrowserModules = Map[String, ContentBrowserConverterModule](
      ImageConverter.typeName -> ImageConverter,
      H5PConverter.typeName -> H5PConverter,
      LenkeConverter.typeName -> LenkeConverter,
      OppgaveConverter.typeName -> OppgaveConverter,
      FagstoffConverter.typeName -> FagstoffConverter,
      AudioConverter.typeName -> AudioConverter,
      FilConverter.typeName -> FilConverter,
      AktualitetConverter.typeName -> AktualitetConverter,
      NonExistentNodeConverter.typeName -> NonExistentNodeConverter,
      VideoConverter.typeName -> VideoConverter,
      VeiledningConverter.typeName -> VeiledningConverter,
      BiblioConverter.typeName -> BiblioConverter)

    def convert(languageContent: LanguageContent, importStatus: ImportStatus): (LanguageContent, ImportStatus) = {
      @tailrec def convert(element: Element, languageContent: LanguageContent, importStatus: ImportStatus, contentBrowserIndex: Int): (LanguageContent, ImportStatus) = {
        val text = element.html()
        val cont = ContentBrowser(text, languageContent.language, contentBrowserIndex)

        if (!cont.isContentBrowserField)
          return (languageContent, importStatus)

        val nodeType = extractService.getNodeType(cont.get("nid")).getOrElse(NonExistentNodeConverter.typeName)

        val (newContent, reqLibs, status) = contentBrowserModules.get(nodeType) match {
          case Some(module) => module.convert(cont, importStatus.visitedNodes)
          case None => {
            val errorString = s"{Unsupported content $nodeType: ${cont.get("nid")}}"
            logger.warn(errorString)
            (errorString, List(), ImportStatus(List(errorString), importStatus.visitedNodes))
          }
        }

        val (start, end) = cont.getStartEndIndex()
        element.html(text.substring(0, start) + newContent + text.substring(end))

        val updatedRequiredLibraries = languageContent.requiredLibraries ++ reqLibs
        val updatedImportStatusMessages = importStatus.messages ++ status.messages
        convert(element, languageContent.copy(requiredLibraries=updatedRequiredLibraries), status.copy(messages=updatedImportStatusMessages), contentBrowserIndex + 1)
      }

      val element = stringToJsoupDocument(languageContent.content)
      val (updatedLanguageContent, updatedImportStatus) = convert(element, languageContent, importStatus, 1)
      (updatedLanguageContent.copy(content=jsoupDocumentToString(element)), updatedImportStatus)
    }
  }
}
