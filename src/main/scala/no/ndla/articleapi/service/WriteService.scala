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
    def newArticleV2(newArticle: api.NewArticleV2): Try[ArticleV2] = {
      validateAndConvertNewArticle(newArticle) match {
        case Success(domainArticle) =>
          val article = articleRepository.newArticle(domainArticle)
          articleIndexService.indexDocument(article)
          Success(converterService.toApiArticleV2(article, newArticle.language).get)
        case Failure(exception) => Failure(exception)
      }
    }

    def validateAndConvertNewArticle(newArticle: api.NewArticleV2): Try[Article] = {
      contentValidator.validateArticle(converterService.toDomainArticle(newArticle), allowUnknownLanguage = false)
    }

    def validateAndConvertUpdatedArticle(articleId: Long, updatedApiArticle: UpdatedArticleV2): Try[Article] = {
      articleRepository.withId(articleId) match {
        case None => Failure(NotFoundException(s"Article with id $articleId does not exist"))
        case Some(existing) =>
          val domainArticle = converterService.toDomainArticle(existing, updatedApiArticle)
          contentValidator.validateArticle(domainArticle, allowUnknownLanguage = true)
      }
    }

    def updateArticleV2(articleId: Long, updatedApiArticle: api.UpdatedArticleV2): Try[api.ArticleV2] = {
      val article = for {
        toUpdate <- validateAndConvertUpdatedArticle(articleId, updatedApiArticle)
        domainArticle <- articleRepository.updateArticle(toUpdate)
        _ <- articleIndexService.indexDocument(domainArticle)
      } yield domainArticle

      article.map(a => converterService.toApiArticleV2(readService.addUrlsOnEmbedResources(a), updatedApiArticle.language).get)
    }

    def allocateArticleId(externalId: Option[String], externalSubjectIds: Set[String] = Set.empty): Long = {
      val repo = articleRepository
      externalId match {
        case None => repo.allocateArticleId
        case Some(nid) => repo.getIdFromExternalId(nid)
          .getOrElse(repo.allocateArticleIdWithExternal(nid, externalSubjectIds))
      }
    }

    def allocateConceptId(externalId: Option[String], externalSubjectIds: Set[String] = Set.empty): Long = {
      val repo = conceptRepository
      externalId match {
        case None => repo.allocateConceptId
        case Some(nid) => repo.getIdFromExternalId(nid).getOrElse(repo.allocateConceptIdWithExternal(nid))
      }
    }

    def newConcept(newConcept: NewConcept): Try[api.Concept] = {
      val concept = converterService.toDomainConcept(newConcept)
      for {
        _ <- importValidator.validate(concept)
        persistedConcept <- Try(conceptRepository.insert(concept))
        _ <- conceptIndexService.indexDocument(concept)
      } yield converterService.toApiConcept(persistedConcept, newConcept.language)
    }

    def updateConcept(id: Long, updateConcept: Concept): Try[api.Concept] = {
      val lang = updateConcept.title.headOption.map(_.language)
      conceptRepository.withId(id) match {
        case None => Failure(NotFoundException(s"Concept with id $id does not exist"))
        case Some(_) =>
          for {
            _ <- importValidator.validate(updateConcept)
            persistedConcept <- conceptRepository.update(updateConcept, id)
            _ <- conceptIndexService.indexDocument(updateConcept)
          } yield converterService.toApiConcept(persistedConcept, lang.getOrElse(Language.AllLanguages))
      }
    }

    def updateConcept(id: Long, updateConcept: api.UpdatedConcept): Try[api.Concept] = {
      conceptRepository.withId(id) match {
        case None => Failure(NotFoundException(s"Concept with id $id does not exist"))
        case Some(concept) =>
          val domainConcept = converterService.toDomainConcept(concept, updateConcept)
          for {
            _ <- importValidator.validate(domainConcept)
            persistedConcept <- conceptRepository.update(domainConcept, id)
            _ <- conceptIndexService.indexDocument(concept)
          } yield converterService.toApiConcept(persistedConcept, updateConcept.language)
      }
    }

  }
}
