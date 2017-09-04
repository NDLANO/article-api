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
import no.ndla.articleapi.model.api.{ArticleV2, NotFoundException}
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.service.search.ArticleIndexService
import no.ndla.articleapi.validation.ContentValidator

import scala.util.{Failure, Success, Try}

trait WriteService {
  this: ArticleRepository with ConverterService with ContentValidator with ArticleIndexService with Clock with User with ReadService =>
  val writeService: WriteService

  class WriteService {
    def newArticle(newArticle: api.NewArticle): Try[api.Article] = {
      val domainArticle = converterService.toDomainArticle(newArticle)
      contentValidator.validateArticle(domainArticle) match {
        case Success(_) =>
          val article = articleRepository.insert(domainArticle)
          articleIndexService.indexDocument(article)
          Success(converterService.toApiArticle(article))
        case Failure(exception) => Failure(exception)
      }
    }

    def newArticleV2(newArticle: api.NewArticleV2): Try[ArticleV2] = {
      val domainArticle = converterService.toDomainArticle(newArticle)
      contentValidator.validateArticle(domainArticle) match {
        case Success(_) => {
          val article = articleRepository.insert(domainArticle)
          articleIndexService.indexDocument(article)
          Success(converterService.toApiArticleV2(article, newArticle.language).get)
        }
        case Failure(exception) => Failure(exception)
      }
    }

    private[service] def mergeLanguageFields[A <: LanguageField[String]](existing: Seq[A], updated: Seq[A]): Seq[A] = {
      val toKeep = existing.filterNot(item => updated.map(_.language).contains(item.language))
      (toKeep ++ updated).filterNot(_.value.isEmpty)
    }

    private def mergeTags(existing: Seq[ArticleTag], updated: Seq[ArticleTag]): Seq[ArticleTag] = {
      val toKeep = existing.filterNot(item => updated.map(_.language).contains(item.language))
      (toKeep ++ updated).filterNot(_.tags.isEmpty)
    }

    private def updateArticle(articleId: Long, updatedApiArticle: api.UpdatedArticle): Try[Article] = {
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
            updatedBy = authUser.id()
          )
          for {
            _ <- contentValidator.validate(toUpdate)
            article <- articleRepository.update(toUpdate)
            _ <- articleIndexService.indexDocument(article)
          } yield readService.addUrlsOnEmbedResources(article)
        }
      }
    }

    def updateArticleV1(articleId: Long, updatedApiArticle: api.UpdatedArticle): Try[api.Article] = {
      updateArticle(articleId, updatedApiArticle).map(converterService.toApiArticle)
    }

    def updateArticleV2(articleId: Long, updatedApiArticle: api.UpdatedArticleV2): Try[api.ArticleV2] = {
      val v1UpdatedArticle = converterService.toUpdatedArticle(updatedApiArticle)
      updateArticle(articleId, v1UpdatedArticle).map(article => converterService.toApiArticleV2(article, updatedApiArticle.language).get)
    }

  }
}
