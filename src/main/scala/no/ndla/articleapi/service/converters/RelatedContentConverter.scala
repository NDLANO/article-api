/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.service.converters

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.integration.ConverterModule.{jsoupDocumentToString, stringToJsoupDocument}
import no.ndla.articleapi.integration.{ConverterModule, LanguageContent, MigrationApiClient}
import no.ndla.articleapi.model.api.ImportException
import no.ndla.articleapi.model.domain.{Article, Concept, ImportStatus}
import no.ndla.articleapi.service.ExtractConvertStoreContent

import scala.util.{Failure, Success, Try}

trait RelatedContentConverter {
  this: ExtractConvertStoreContent with HtmlTagGenerator with MigrationApiClient with LazyLogging =>

  object RelatedContentConverter extends ConverterModule {
    override def convert(content: LanguageContent, importStatus: ImportStatus): Try[(LanguageContent, ImportStatus)] = {
      val nids = content.relatedContent.map(_.nid).toSet

      if (nids.isEmpty) {
        Success(content, importStatus)
      } else {
        importRelatedContent(nids) match {
          case Success(relatedEmbed) =>
            val element = stringToJsoupDocument(content.content)
            element.append(s"<section>$relatedEmbed</section>")
            Success(content.copy(content = jsoupDocumentToString(element)), importStatus)
          case Failure(ex) => Failure(ex)
        }
      }

    }

  }

  private def importRelatedContent(relatedNids: Set[String]): Try[String] = {
    val (importSuccesses, importFailures) = relatedNids.map(nid => extractConvertStoreContent.processNode(nid)).map {
      case Success((content: Article, _)) => Success(content.id.get)
      case Success((_: Concept, _)) => Failure(ImportException("Related content points to a concept. This should not be legal, no?"))
      case Failure(ex) => Failure(ex)
    }.partition(_.isSuccess)

    if (importFailures.isEmpty) {
      Success(HtmlTagGenerator.buildRelatedContent(importSuccesses.map(_.get)))
    } else {
      val importErrorMsgs = importFailures.map(_.failed.get.getMessage).mkString(", ")
      val exceptionMsg = s"Failed to import one or more related contents: $importErrorMsgs"

      logger.info(exceptionMsg)
      Failure(ImportException(exceptionMsg))
    }
  }

}
