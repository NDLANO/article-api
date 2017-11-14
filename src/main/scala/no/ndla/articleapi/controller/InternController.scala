/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.controller

import java.util.concurrent.TimeUnit

import no.ndla.articleapi.model.api.ArticleIdV2
import no.ndla.articleapi.model.domain.{ImportStatus, Language}
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.service._
import no.ndla.articleapi.service.search.{ArticleIndexService, ConceptIndexService, IndexService}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.{InternalServerError, NotFound, Ok}

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

trait InternController {
  this: ReadService
    with WriteService
    with ExtractService
    with ConverterService
    with ArticleRepository
    with ArticleContentInformation
    with ExtractConvertStoreContent
    with IndexService
    with ArticleIndexService
    with ConceptIndexService =>
  val internController: InternController

  class InternController extends NdlaController {

    protected implicit override val jsonFormats: Formats = DefaultFormats

    post("/index") {
      val indexResults = for {
        articleIndex <- Future { articleIndexService.indexDocuments }
        conceptIndex <- Future { conceptIndexService.indexDocuments }
      } yield (articleIndex, conceptIndex)


      Await.result(indexResults, Duration(1, TimeUnit.MINUTES)) match {
        case (Success(articleResult), Success(conceptResult)) =>
          val indexTime = math.max(articleResult.millisUsed, conceptResult.millisUsed)
          val result = s"Completed indexing of ${articleResult.totalIndexed} articles and ${conceptResult.totalIndexed} concepts in $indexTime ms."
          logger.info(result)
          Ok(result)
        case (Failure(articleFail), _) =>
          logger.warn(articleFail.getMessage, articleFail)
          InternalServerError(articleFail.getMessage)
        case (_, Failure(conceptFail)) =>
          logger.warn(conceptFail.getMessage, conceptFail)
          InternalServerError(conceptFail.getMessage)
      }
    }

    post("/import/:external_id") {
      val externalId = params("external_id")
      val forceUpdateArticle = booleanOrDefault("forceUpdate", false)

      extractConvertStoreContent.processNode(externalId, forceUpdateArticle) match {
        case Success((content, status)) => status.addMessage(s"Successfully imported node $externalId: ${content.id.get}")
        case Failure(exc) => errorHandler(exc)
      }
    }

    get("/ids") {
      articleRepository.getAllIds
    }

    get("/id/:external_id") {
      val externalId = params("external_id")
      articleRepository.getIdFromExternalId(externalId) match {
        case Some(id) => id
        case None => NotFound()
      }
    }

    post("/id/article/allocate/?") {
      val externalId = paramOrNone("external-id")
      val externalSubjectId = paramAsListOfString("external-subject-id")
      ArticleIdV2(writeService.allocateArticleId(externalId, externalSubjectId.toSet))
    }

    post("/id/concept/allocate/?") {
      val externalId = paramOrNone("external-id")
      val externalSubjectId = paramAsListOfString("external-subject-id")
      ArticleIdV2(writeService.allocateConceptId(externalId, externalSubjectId.toSet))
    }

    get("/tagsinuse") {
      ArticleContentInformation.getHtmlTagsMap
    }

    get("/embedurls/:external_subject_id") {
      ArticleContentInformation.getExternalEmbedResources(params("external_subject_id"))
    }

    get("/reports/headerElementsInLists") {
      contentType = "text/csv"
      ArticleContentInformation.getFaultyHtmlReport
    }

    get("/articles") {
      val pageNo = intOrDefault("page", 1)
      val pageSize = intOrDefault("page-size", 250)
      val lang = paramOrDefault("language", Language.AllLanguages)

      readService.getArticlesByPage(pageNo, pageSize, lang)
    }

  }
}
