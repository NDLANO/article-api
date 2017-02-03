/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.model.domain.{ImportStatus, RequiredLibrary}
import no.ndla.articleapi.service.ExtractService
import no.ndla.articleapi.service.converters.HtmlTagGenerator
import no.ndla.articleapi.ArticleApiProperties.H5PResizerScriptUrl

import scala.util.{Success, Try}

trait H5PConverterModule {
  this: ExtractService with HtmlTagGenerator =>

  object H5PConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "h5p_content"

    override def convert(content: ContentBrowser, visitedNodes: Seq[String]): Try[(String, Seq[RequiredLibrary], ImportStatus)] = {
      val nodeId = content.get("nid")

      logger.info(s"Converting h5p_content with nid $nodeId")
      val requiredLibraries = List(RequiredLibrary("text/javascript", "H5P-Resizer", H5PResizerScriptUrl))
      val replacement = HtmlTagGenerator.buildH5PEmbedContent(s"http://ndla.no/h5p/embed/$nodeId")
      Success(replacement, requiredLibraries, ImportStatus(Seq(), visitedNodes))
    }
  }
}
