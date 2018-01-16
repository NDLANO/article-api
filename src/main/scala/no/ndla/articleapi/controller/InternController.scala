/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.controller

import java.util.concurrent.TimeUnit

import no.ndla.articleapi.auth.{Role, User}
import no.ndla.articleapi.model.api.{ArticleIdV2, UpdatedConcept}
import no.ndla.articleapi.model.domain.{Concept, Language}
import no.ndla.articleapi.model.api.ArticleIdV2
import no.ndla.articleapi.model.domain.{Article, Language}
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.service._
import no.ndla.articleapi.service.search.{ArticleIndexService, ConceptIndexService, IndexService}
import no.ndla.articleapi.validation.ContentValidator
import no.ndla.validation.ValidationException
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.{InternalServerError, NotFound, Ok}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

trait InternController {
  this: ReadService
    with WriteService
    with ConverterService
    with ArticleRepository
    with IndexService
    with ArticleIndexService
    with ConceptIndexService
    with User
    with Role
    with ContentValidator =>
  val internController: InternController

  class InternController extends NdlaController {

    protected implicit override val jsonFormats: Formats = DefaultFormats

    post("/index") {
      val indexResults = for {
        articleIndex <- Future { articleIndexService.indexDocuments }
        conceptIndex <- Future { conceptIndexService.indexDocuments }
      } yield (articleIndex, conceptIndex)


      Await.result(indexResults, Duration(10, TimeUnit.MINUTES)) match {
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
      authRole.assertHasDraftWritePermission()

      val externalId = paramOrNone("external-id")
      val externalSubjectId = paramAsListOfString("external-subject-id")
      ArticleIdV2(writeService.allocateArticleId(externalId, externalSubjectId.toSet))
    }

    post("/id/concept/allocate/?") {
      authRole.assertHasDraftWritePermission()
      val externalId = paramOrNone("external-id")
      ArticleIdV2(writeService.allocateConceptId(externalId))
    }

    get("/articles") {
      val pageNo = intOrDefault("page", 1)
      val pageSize = intOrDefault("page-size", 250)
      val lang = paramOrDefault("language", Language.AllLanguages)

      readService.getArticlesByPage(pageNo, pageSize, lang)
    }

    post("/validate/article") {
      val article = extract[Article](request.body)
      contentValidator.validateArticle(article, allowUnknownLanguage = true) match {
        case Success(_) => article
        case Failure(ex: ValidationException) => ex.errors
        case Failure(ex) => errorHandler(ex)
      }
    }

    post("/article/:id") {
      authRole.assertHasWritePermission()
      val article = extract[Article](request.body)
      val id = long("id")

      writeService.updateArticle(article.copy(id=Some(id))) match {
        case Success(a) => a
        case Failure(ex) => errorHandler(ex)
      }
    }

    post("/concept/:id") {
      authRole.assertHasWritePermission()
      val id = long("id")
      val concept = extract[Concept](request.body)

      writeService.updateConcept(id, concept) match {
        case Success(c) => c
        case Failure(ex) => errorHandler(ex)
      }
    }

  }
}
