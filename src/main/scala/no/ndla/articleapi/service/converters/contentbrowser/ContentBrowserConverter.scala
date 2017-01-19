/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import org.jsoup.nodes.Element
import scala.annotation.tailrec
import no.ndla.articleapi.integration.{ConverterModule, LanguageContent}
import no.ndla.articleapi.model.domain.ImportStatus
import no.ndla.articleapi.ArticleApiProperties.EnableJoubelH5POembed
import no.ndla.articleapi.integration.ConverterModule.{stringToJsoupDocument, jsoupDocumentToString}

trait ContentBrowserConverter {
  this: ContentBrowserConverterModules =>
  val contentBrowserConverter: ContentBrowserConverter

  class ContentBrowserConverter extends ConverterModule with LazyLogging {
    private val contentBrowserModules = Map[String, ContentBrowserConverterModule](
      ImageConverter.typeName -> ImageConverter,
      if (EnableJoubelH5POembed) JoubelH5PConverter.typeName -> JoubelH5PConverter else H5PConverter.typeName -> H5PConverter,
      LenkeConverter.typeName -> LenkeConverter,
      OppgaveConverter.typeName -> OppgaveConverter,
      FagstoffConverter.typeName -> FagstoffConverter,
      AktualitetConverter.typeName -> AktualitetConverter,
      NonExistentNodeConverter.typeName -> NonExistentNodeConverter,
      VideoConverter.typeName -> VideoConverter,
      VeiledningConverter.typeName -> VeiledningConverter,
      AudioConverter.typeName -> AudioConverter,
      BiblioConverter.typeName -> BiblioConverter)

    private def getConverterModule(contentBrowser: ContentBrowser) = {
      val nodeType = extractService.getNodeType(contentBrowser.get("nid")).getOrElse(NonExistentNodeConverter.typeName)
      contentBrowserModules.getOrElse(nodeType, UnsupportedContentConverter)
    }

    def convert(languageContent: LanguageContent, importStatus: ImportStatus): (LanguageContent, ImportStatus) = {
      @tailrec def convert(element: Element, languageContent: LanguageContent, importStatus: ImportStatus): (LanguageContent, ImportStatus) = {
        val text = element.html()
        val cont = ContentBrowser(text, languageContent.language)

        if (!cont.isContentBrowserField)
          return (languageContent, importStatus)

        val (newContent, reqLibs, status) = getConverterModule(cont).convert(cont, importStatus.visitedNodes)

        val (start, end) = cont.getStartEndIndex()
        element.html(text.substring(0, start) + newContent + text.substring(end))

        val updatedRequiredLibraries = languageContent.requiredLibraries ++ reqLibs
        val updatedImportStatusMessages = importStatus.messages ++ status.messages
        convert(element, languageContent.copy(requiredLibraries=updatedRequiredLibraries),
          status.copy(messages=updatedImportStatusMessages))
      }

      val contentElement = stringToJsoupDocument(languageContent.content)
      val (updatedLanguageContent, updatedImportStatus) = convert(contentElement, languageContent, importStatus)

      val metaDescriptionElement = stringToJsoupDocument(languageContent.metaDescription)
      val (finalLanguageContent, finalImportStatus) = convert(metaDescriptionElement, updatedLanguageContent, updatedImportStatus)

      (finalLanguageContent.copy(content=jsoupDocumentToString(contentElement), metaDescription=jsoupDocumentToString(metaDescriptionElement)),
        finalImportStatus)
    }
  }
}
