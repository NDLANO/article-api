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
import no.ndla.articleapi.integration.H5PApiClient
import no.ndla.articleapi.model.api.ImportException

import scala.util.{Failure, Success, Try}

trait H5PConverterModule {
  this: ExtractService with HtmlTagGenerator with H5PApiClient =>

  object H5PConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "h5p_content"

    override def convert(content: ContentBrowser, importStatus: ImportStatus): Try[(String, Seq[RequiredLibrary], ImportStatus)] = {
      val nodeId = content.get("nid")

      logger.info(s"Converting h5p_content with nid $nodeId")
      toH5PEmbed(nodeId) match {
        case Success(replacement) =>
          Success((replacement, Seq.empty, importStatus))
        case Failure(ex) =>
          Failure(ex)
      }
    }

    def toH5PEmbed(nodeId: String): Try[String] = {
      val requiredLibrary = RequiredLibrary("text/javascript", "H5P-Resizer", H5PResizerScriptUrl) //TODO: most likely remove this
      h5pApiClient.getOembedFromOldId(nodeId) match {
        case Some(url) =>
          val replacement = HtmlTagGenerator.buildH5PEmbedContent(url)
          Success(replacement)
        case None => Failure(ImportException(message = "Failed to import H5P"))
      }
    }
  }
}
