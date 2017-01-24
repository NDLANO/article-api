/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import no.ndla.articleapi.model.api
import no.ndla.articleapi.model.api.NotFoundException
import no.ndla.articleapi.model.domain
import no.ndla.articleapi.model.domain.LanguageField
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.service.search.IndexService
import no.ndla.articleapi.validation.ArticleValidator

import scala.util.{Failure, Try}

trait WriteService {
  this: ArticleRepository with ConverterService with ArticleValidator with IndexService with Clock =>
  val writeService: WriteService

  class WriteService {
    def newArticle(newArticle: api.NewArticle) = {
      val domainArticle = converterService.toDomainArticle(newArticle)
      articleValidator.validateArticle(domainArticle)
      val article = articleRepository.insert(domainArticle)
      indexService.indexDocument(article)
      converterService.toApiArticle(article)
    }

    private[service] def mergeLanguageFields[A <: LanguageField](existing: Seq[A], updated: Seq[A]): Seq[A] = {
      val toKeep = existing.filterNot(item => updated.map(_.language).contains(item.language))
      (toKeep ++ updated).filterNot(_.value.isEmpty)
    }

    private def mergeTags(existing: Seq[domain.ArticleTag], updated: Seq[domain.ArticleTag]): Seq[domain.ArticleTag] = {
      val toKeep = existing.filterNot(item => updated.map(_.language).contains(item.language))
      (toKeep ++ updated).filterNot(_.tags.isEmpty)
    }

    def updateArticle(articleId: Long, updatedApiArticle: api.UpdatedArticle): Try[api.Article] = {
      articleRepository.withId(articleId) match {
        case None => Failure(NotFoundException(s"Article with id $articleId does not exist"))
        case Some(existing) => {
          val updatedArticle = converterService.toDomainArticle(updatedApiArticle)
          val toUpdate = existing.copy(
            revision = updatedArticle.revision,
            title = mergeLanguageFields(existing.title, updatedArticle.title),
            content = mergeLanguageFields(existing.content, updatedArticle.content),
            copyright = updatedArticle.copyright,
            tags = mergeTags(existing.tags, updatedArticle.tags),
            requiredLibraries = updatedArticle.requiredLibraries,
            visualElement = mergeLanguageFields(existing.visualElement, updatedArticle.visualElement),
            introduction = mergeLanguageFields(existing.introduction, updatedArticle.introduction),
            metaDescription = mergeLanguageFields(existing.metaDescription, updatedArticle.metaDescription),
            metaImageId = updatedArticle.metaImageId,
            updated = clock.now(),
            contentType = updatedArticle.contentType
          )
          articleValidator.validateArticle(toUpdate)
          for {
            article <- articleRepository.update(toUpdate)
            x <- indexService.indexDocument(article)
          } yield converterService.toApiArticle(article)
         }
      }
    }

  }
}
