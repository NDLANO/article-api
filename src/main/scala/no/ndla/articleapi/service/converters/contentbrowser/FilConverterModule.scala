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
import no.ndla.articleapi.model.domain.{ImportStatus, RequiredLibrary}
import no.ndla.articleapi.service.converters.HtmlTagGenerator
import no.ndla.articleapi.service.{AttachmentStorageService, ExtractService}
import no.ndla.articleapi.ArticleApiProperties.Domain

import scala.util.{Failure, Success, Try}

trait FilConverterModule {
  this: ExtractService with AttachmentStorageService with HtmlTagGenerator =>

  object FilConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "fil"

    override def convert(content: ContentBrowser, visitedNodes: Seq[String]): Try[(String, Seq[RequiredLibrary], ImportStatus)] = {
      val nodeId = content.get("nid")
      val importedFile = for {
        fileMeta <- extractService.getNodeFilMeta(nodeId)
        filePath <- attachmentStorageService.uploadFileFromUrl(nodeId, fileMeta)
      } yield (HtmlTagGenerator.buildAnchor(s"$Domain/files/$filePath", fileMeta.fileName, fileMeta.fileName, openInNewTab=false), Seq.empty, ImportStatus(Seq.empty, visitedNodes))

      importedFile match {
        case Success(x) => Success(x)
        case Failure(x) => Failure(ImportException(s"Failed to import file with node id $nodeId: ${x.getMessage}"))
      }
    }

  }
}
