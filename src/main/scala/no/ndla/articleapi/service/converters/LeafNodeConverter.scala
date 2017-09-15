/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */


package no.ndla.articleapi.service.converters

import no.ndla.articleapi.ArticleApiProperties.{nodeTypeH5P, nodeTypeVideo}
import no.ndla.articleapi.integration.{ConverterModule, LanguageContent, MigrationApiClient}
import no.ndla.articleapi.model.domain.ImportStatus
import no.ndla.articleapi.service.{ExtractService, TagsService}
import no.ndla.articleapi.service.converters.contentbrowser.{H5PConverterModule, VideoConverterModule}
import no.ndla.network.NdlaClient

import scala.util.{Success, Try}

trait LeafNodeConverter {
  this: VideoConverterModule with HtmlTagGenerator with H5PConverterModule with ExtractService with MigrationApiClient with TagsService with NdlaClient with TagsService =>

  object LeafNodeConverter extends ConverterModule {

    def convert(content: LanguageContent, importStatus: ImportStatus): Try[(LanguageContent, ImportStatus)] = {
      val (articleContent, requiredLibraries) = content.nodeType match {
        case `nodeTypeVideo` =>
          val (html, requiredLibrary) = VideoConverter.toVideo("", content.nid)
          (s"<section>$html</section>", Set(requiredLibrary) ++ content.requiredLibraries)
        case `nodeTypeH5P` =>
          val (html, requiredLibrary) = H5PConverter.toH5PEmbed(content.nid)
          (s"<section>$html</section>", Set(requiredLibrary) ++ content.requiredLibraries)
        case _ => (content.content, content.requiredLibraries)
      }

      Success(content.copy(content = articleContent, requiredLibraries=requiredLibraries), importStatus)
    }

  }
}
