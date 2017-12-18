/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.model.api.ImportException
import no.ndla.articleapi.model.domain.{FootNoteItem, ImportStatus, RequiredLibrary}
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.service.converters.HtmlTagGenerator
import no.ndla.articleapi.service.ExtractService

import scala.util.{Failure, Success, Try}

trait BiblioConverterModule {
  this: ExtractService with ArticleRepository with HtmlTagGenerator =>

  object BiblioConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "biblio"

    override def convert(content: ContentBrowser, importStatus: ImportStatus): Try[(String, Seq[RequiredLibrary], ImportStatus)] = {
      val nodeId = content.get("nid")
      getFootNoteData(nodeId) match {
        case None => Failure(ImportException(s"Failed to fetch biblio meta data with node id $nodeId"))
        case Some(meta) => Success(HtmlTagGenerator.buildFootNoteItem(
          title = meta.title,
          `type` = meta.`type`,
          year = meta.year,
          edition = meta.edition,
          publisher = meta.publisher,
          authors = meta.authors
        ), List[RequiredLibrary](), importStatus)
      }
    }

    private def getFootNoteData(nodeId: String): Option[FootNoteItem] =
      extractService.getBiblioMeta(nodeId).map(biblioMeta => FootNoteItem(biblioMeta.biblio, biblioMeta.authors))

  }
}
