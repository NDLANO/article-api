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
import no.ndla.articleapi.model.api.{ArticleContentV2, ArticleV2, NotFoundException}
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.service.search.ArticleIndexService
import no.ndla.articleapi.validation.ContentValidator

import scala.util.{Failure, Success, Try}

trait WriteService {
  this: ArticleRepository with ConverterService with ContentValidator with ArticleIndexService with Clock with User with ReadService =>
  val writeService: WriteService

  class WriteService {
    def newArticleV2(newArticle: api.NewArticleV2): Try[ArticleV2] = {
      val domainArticle = converterService.toDomainArticle(newArticle)
      contentValidator.validateArticle(domainArticle, false) match {
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

    private def _updateArticleV2(articleId: Long, updatedApiArticle: api.UpdatedArticleV2): Try[Article] = {
      articleRepository.withId(articleId) match {
        case None => Failure(NotFoundException(s"Article with id $articleId does not exist"))
        case Some(existing) => {
          val lang = updatedApiArticle.language

          val toUpdate = existing.copy(
            revision = Option(updatedApiArticle.revision),
            title = mergeLanguageFields(existing.title, updatedApiArticle.title.toSeq.map(t => converterService.toDomainTitle(api.ArticleTitle(t, lang)))),
            content = mergeLanguageFields(existing.content, updatedApiArticle.content.toSeq.map(c => converterService.toDomainContent(api.ArticleContentV2(c, lang)))),
            copyright = updatedApiArticle.copyright.map(c => converterService.toDomainCopyright(c)).getOrElse(existing.copyright),
            tags = mergeTags(existing.tags, converterService.toDomainTagV2(updatedApiArticle.tags, lang)),
            //TODO: Look over the below ones
            requiredLibraries = updatedApiArticle.requiredLibraries.map(converterService.toDomainRequiredLibraries),
            visualElement = mergeLanguageFields(existing.visualElement, converterService.toDomainVisualElementV2(updatedApiArticle.visualElement, lang)),
            introduction = mergeLanguageFields(existing.introduction, converterService.toDomainIntroductionV2(updatedApiArticle.introduction, lang)),
            metaDescription = mergeLanguageFields(existing.metaDescription, converterService.toDomainMetaDescriptionV2(updatedApiArticle.metaDescription, lang)),

            metaImageId = if (updatedApiArticle.metaImageId.isDefined) updatedApiArticle.metaImageId else existing.metaImageId,
            updated = clock.now(),
            updatedBy = authUser.id()
          )
          /*
          val v1Title = Seq(api.ArticleTitle(updatedApiArticle.title.getOrElse(""), lang))
          val toUpdatev1 = existing.copy(
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
          */



          for {
            _ <- contentValidator.validateArticle(toUpdate, allowUnknownLanguage = true)
            article <- articleRepository.update(toUpdate)
            _ <- articleIndexService.indexDocument(article)
          } yield readService.addUrlsOnEmbedResources(article)
        }
      }
    }
    def updateArticleV2(articleId: Long, updatedApiArticle: api.UpdatedArticleV2): Try[api.ArticleV2] = {
      _updateArticleV2(articleId, updatedApiArticle).map(article => converterService.toApiArticleV2(article, updatedApiArticle.language).get)
    }
  }
}
