/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */


package no.ndla.articleapi.service.converters

import no.ndla.articleapi.ArticleApiProperties.{nodeTypeH5P, nodeTypeVideo}
import no.ndla.articleapi.integration.ConverterModule.{jsoupDocumentToString, stringToJsoupDocument}
import no.ndla.articleapi.integration.{ConverterModule, LanguageContent, MigrationApiClient}
import no.ndla.articleapi.model.domain.{ImportStatus, RequiredLibrary}
import no.ndla.articleapi.service.converters.contentbrowser.{H5PConverterModule, VideoConverterModule}
import no.ndla.articleapi.service.{ExtractService, TagsService}
import no.ndla.network.NdlaClient
import org.jsoup.nodes.Element

import scala.util.{Failure, Success, Try}

trait LeafNodeConverter {
  this: VideoConverterModule with HtmlTagGenerator with H5PConverterModule with ExtractService with MigrationApiClient with TagsService with NdlaClient with TagsService =>

  object LeafNodeConverter extends ConverterModule {

    def convert(content: LanguageContent, importStatus: ImportStatus): Try[(LanguageContent, ImportStatus)] = {
      val element = stringToJsoupDocument(content.content)

      val requiredLibraries = content.nodeType match {
        case `nodeTypeVideo` =>
          val html = VideoConverter.toInlineVideo("", content.nid)
          element.prepend(s"<section>$html</section>")
          Success(content.requiredLibraries)
        case `nodeTypeH5P` =>
          H5PConverter.toH5PEmbed(content.nid) match {
            case Success(html) =>
              element.prepend(s"<section>$html</section>")
              Success(content.requiredLibraries)
            case Failure(ex) => Failure(ex)
          }
        case _ => Success(content.requiredLibraries)
      }

      requiredLibraries match {
        case Success(requiredLib) =>
          Success(content.copy(content=jsoupDocumentToString(element), requiredLibraries=requiredLib), importStatus)
        case Failure(ex) =>
          Failure(ex)
      }

    }

  }
}
