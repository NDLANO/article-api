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
import no.ndla.articleapi.model.domain._
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

    private def mergeTags(existing: Seq[ArticleTag], updated: Seq[ArticleTag]): Seq[ArticleTag] = {
      val toKeep = existing.filterNot(item => updated.map(_.language).contains(item.language))
      (toKeep ++ updated).filterNot(_.tags.isEmpty)
    }

    def updateArticle(articleId: Long, updatedApiArticle: api.UpdatedArticle): Try[api.Article] = {
      articleRepository.withId(articleId) match {
        case None => Failure(NotFoundException(s"Article with id $articleId does not exist"))
        case Some(existing) => {
          val toUpdate = existing.copy(
            revision = Option(updatedApiArticle.revision),
            title = mergeLanguageFields(existing.title, updatedApiArticle.title.map(converterService.toDomainTitle)),
            content = mergeLanguageFields(existing.content, updatedApiArticle.content.map(converterService.toDomainContent)),
            copyright = updatedApiArticle.copyright.map(converterService.toDomainCopyright).getOrElse(existing.copyright),
            tags = mergeTags(existing.tags, updatedApiArticle.tags.map(converterService.toDomainTag)),
            requiredLibraries = updatedApiArticle.requiredLibraries.map(converterService.toDomainRequiredLibraries),
            visualElement = mergeLanguageFields(existing.visualElement, updatedApiArticle.visualElement.map(converterService.toDomainVisualElement)),
            introduction = mergeLanguageFields(existing.introduction, updatedApiArticle.introduction.map(converterService.toDomainIntroduction)),
            metaDescription = mergeLanguageFields(existing.metaDescription, updatedApiArticle.metaDescription.map(converterService.toDomainMetaDescription)),
            metaImageId = if(updatedApiArticle.metaImageId.isDefined) updatedApiArticle.metaImageId else existing.metaImageId,
            updated = clock.now(),
            contentType = updatedApiArticle.contentType.getOrElse(existing.contentType)
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
