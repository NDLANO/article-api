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
    with ReadService
    with ArticleIndexService
    with ConceptIndexService =>
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

    def updateArticle(article: Article, externalIds: List[Long]): Try[Article] = {
      for {
        _ <- contentValidator.validateArticle(article, allowUnknownLanguage = true)
        domainArticle <- articleRepository.updateArticleFromDraftApi(article, externalIds.map(_.toString))
        _ <- articleIndexService.indexDocument(domainArticle)
      } yield domainArticle
    }

    def allocateArticleId(externalIds: List[String], externalSubjectIds: Set[String] = Set.empty): Long = {
      val repo = articleRepository
      externalIds match {
        case Nil => repo.allocateArticleId
        case mainNid :: restOfNids =>
          repo
            .getIdFromExternalId(mainNid)
            .getOrElse(repo.allocateArticleIdWithExternalIds(mainNid :: restOfNids, externalSubjectIds))
      }
    }

    def allocateConceptId(externalIds: List[String]): Long = {
      val repo = conceptRepository
      externalIds match {
        case Nil => repo.allocateConceptId
        case mainNid :: restOfNids =>
          repo
            .getIdFromExternalId(mainNid)
            .getOrElse(repo.allocateConceptIdWithExternalIds(mainNid :: restOfNids))
      }
    }

    def updateConcept(id: Long, concept: Concept): Try[Concept] = {
      for {
        _ <- contentValidator.validateConcept(concept, allowUnknownLanguage = true)
        domainConcept <- conceptRepository.updateConceptFromDraftApi(concept.copy(id = Some(id)))
        _ <- conceptIndexService.indexDocument(domainConcept)
      } yield domainConcept
    }

    def unpublishArticle(id: Long): Try[api.ArticleIdV2] = {
      articleRepository
        .unpublish(id)
        .flatMap(articleIndexService.deleteDocument)
        .map(api.ArticleIdV2)
    }

    def deleteArticle(id: Long): Try[api.ArticleIdV2] = {
      articleRepository
        .delete(id)
        .flatMap(articleIndexService.deleteDocument)
        .map(api.ArticleIdV2)
    }

    def deleteConcept(id: Long): Try[api.ArticleIdV2] = {
      conceptRepository
        .delete(id)
        .flatMap(conceptIndexService.deleteDocument)
        .map(api.ArticleIdV2)
    }

  }
}
