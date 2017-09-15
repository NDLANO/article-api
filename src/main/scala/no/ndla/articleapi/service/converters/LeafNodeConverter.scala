/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */


package no.ndla.articleapi.service.converters

import no.ndla.articleapi.ArticleApiProperties.nodeTypeVideo
import no.ndla.articleapi.integration.{ConverterModule, LanguageContent}
import no.ndla.articleapi.model.domain.ImportStatus
import no.ndla.articleapi.service.converters.contentbrowser.VideoConverterModule

import scala.util.{Success, Try}

trait LeafNodeConverter {
  this: VideoConverterModule with HtmlTagGenerator =>

  object LeafNodeConverter extends ConverterModule {

    def convert(content: LanguageContent, importStatus: ImportStatus): Try[(LanguageContent, ImportStatus)] = {
      val (articleContent, requiredLibraries) = content.nodeType match {
        case `nodeTypeVideo` =>
          val (html, requiredLibrary) = VideoConverter.toVideo("", content.nid)
          (s"<section>$html</section>", Set(requiredLibrary) ++ content.requiredLibraries)
        case _ => (content.content, content.requiredLibraries)
      }

      Success(content.copy(content = articleContent, requiredLibraries=requiredLibraries), importStatus)
    }

  }
}
