/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import no.ndla.articleapi.auth.User
import no.ndla.articleapi.model.api
import no.ndla.articleapi.model.api.{ArticleV2, NewConcept, NotFoundException, UpdatedArticleV2}
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.repository.{ArticleRepository, ConceptRepository}
import no.ndla.articleapi.service.search.{ArticleIndexService, ConceptIndexService}
import no.ndla.articleapi.validation.ContentValidator
import scala.util.{Failure, Success, Try}

trait WriteService {
  this: ArticleRepository
    with ConceptRepository
    with ConceptIndexService
    with ConverterService
    with ContentValidator
    with ArticleIndexService
    with Clock
    with User
    with ReadService =>
  val writeService: WriteService

  class WriteService {
    def validateAndConvertUpdatedArticle(articleId: Long, updatedApiArticle: UpdatedArticleV2): Try[Article] = {
      articleRepository.withId(articleId) match {
        case None => Failure(NotFoundException(s"Article with id $articleId does not exist"))
        case Some(existing) =>
          val domainArticle = converterService.toDomainArticle(existing, updatedApiArticle)
          contentValidator.validateArticle(domainArticle, allowUnknownLanguage = true)
      }
    }

    def updateArticle(article: Article): Try[Article] = {
      for {
        _ <- contentValidator.validateArticle(article, allowUnknownLanguage = true)
        domainArticle <- articleRepository.updateArticleFromDraftApi(article)
        _ <- articleIndexService.indexDocument(domainArticle)
      } yield domainArticle
    }

    def allocateArticleId(externalId: Option[String], externalSubjectIds: Set[String] = Set.empty): Long = {
      val repo = articleRepository
      externalId match {
        case None => repo.allocateArticleId
        case Some(nid) => repo.getIdFromExternalId(nid)
          .getOrElse(repo.allocateArticleIdWithExternal(nid, externalSubjectIds))
      }
    }

    def allocateConceptId(externalId: Option[String]): Long = {
      val repo = conceptRepository
      externalId match {
        case None => repo.allocateConceptId
        case Some(nid) => repo.getIdFromExternalId(nid).getOrElse(repo.allocateConceptIdWithExternal(nid))
      }
    }

    def updateConcept(id: Long, concept: Concept): Try[Concept] = {
      for {
        _ <- contentValidator.validateConcept(concept, allowUnknownLanguage = true)
        domainConcept <- conceptRepository.updateConceptFromDraftApi(concept.copy(id=Some(id)))
        _ <- conceptIndexService.indexDocument(domainConcept)
      } yield domainConcept
    }

  }
}
