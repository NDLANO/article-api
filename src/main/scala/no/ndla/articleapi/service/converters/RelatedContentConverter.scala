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
import no.ndla.articleapi.service.{ExtractConvertStoreContent, ExtractService, ReadService}
import no.ndla.articleapi.ArticleApiProperties.supportedContentTypes
import no.ndla.articleapi.repository.ArticleRepository

import scala.util.{Failure, Success, Try}

trait RelatedContentConverter {
  this: ExtractConvertStoreContent with HtmlTagGenerator with MigrationApiClient with LazyLogging with ExtractService with ArticleRepository =>

  object RelatedContentConverter extends ConverterModule {
    override def convert(content: LanguageContent, importStatus: ImportStatus): Try[(LanguageContent, ImportStatus)] = {
      val nids = content.relatedContent
        .filter(related => supportedContentTypes.contains(extractService.getNodeType(related.nid).getOrElse("unknown")))
        .map(_.nid).toSet

      if (nids.isEmpty) {
        Success(content, importStatus.copy(importRelatedArticles = false))
      } else {
        val handlerFunc = if (importStatus.importRelatedArticles) importRelatedContent _ else getRelatedContentFromDb _

        handlerFunc(nids, importStatus) match {
          case Success((relatedEmbed, status)) =>
            val element = stringToJsoupDocument(content.content)
            element.append(s"<section>$relatedEmbed</section>")
            Success(content.copy(content = jsoupDocumentToString(element)), status)
          case Failure(ex) => Failure(ex)
        }
      }

    }
  }

  private def importRelatedContent(relatedNids: Set[String], importStatus: ImportStatus): Try[(String, ImportStatus)] = {
    val (importedArticles, updatedStatus) = relatedNids.foldLeft((Seq[Try[Article]](), importStatus.copy(importRelatedArticles = false)))((result, nid) => {
      val (articles, status) = result

      extractConvertStoreContent.processNode(nid, status) match {
        case Success((content: Article, st)) =>
          (articles :+ Success(content), st)
        case Success((_: Concept, _)) =>
          (articles :+ Failure(ImportException("Related content points to a concept. This should not be legal, no?")), status)
        case Failure(ex) =>
          (articles :+ Failure(ex), status)
      }
    })

    val (importSuccesses, importFailures) = importedArticles.partition(_.isSuccess)

    if (importFailures.isEmpty) {
      val ids = importSuccesses.map(_.get.id.get).toSet
      Success(HtmlTagGenerator.buildRelatedContent(ids), updatedStatus)
    } else {
      val importErrorMsgs = importFailures.map(_.failed.get.getMessage).mkString(", ")
      val exceptionMsg = s"Failed to import one or more related contents: $importErrorMsgs"

      logger.info(exceptionMsg)
      Failure(ImportException(exceptionMsg))
    }
  }

  private def getRelatedContentFromDb(nids: Set[String], importStatus: ImportStatus): Try[(String, ImportStatus)] = {
    val ids = nids.map(articleRepository.getIdFromExternalId).flatten
    Success((HtmlTagGenerator.buildRelatedContent(ids), importStatus))
  }

}
