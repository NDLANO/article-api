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
import no.ndla.articleapi.model.api.{ArticleV2, NotFoundException, UpdatedArticleV2}
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.repository.{ArticleRepository, ConceptRepository}
import no.ndla.articleapi.service.search.ArticleIndexService
import no.ndla.articleapi.validation.ContentValidator

import scala.util.{Failure, Success, Try}

trait WriteService {
  this: ArticleRepository
    with ConceptRepository
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

    private[service] def mergeLanguageFields[A <: LanguageField[String]](existing: Seq[A], updated: Seq[A]): Seq[A] = {
      val toKeep = existing.filterNot(item => updated.map(_.language).contains(item.language))
      (toKeep ++ updated).filterNot(_.value.isEmpty)
    }

    private def mergeTags(existing: Seq[ArticleTag], updated: Seq[ArticleTag]): Seq[ArticleTag] = {
      val toKeep = existing.filterNot(item => updated.map(_.language).contains(item.language))
      (toKeep ++ updated).filterNot(_.tags.isEmpty)
    }

    def validateAndConvertUpdatedArticle(articleId: Long, updatedApiArticle: UpdatedArticleV2): Try[Article] = {
      articleRepository.withId(articleId) match {
        case None => Failure(NotFoundException(s"Article with id $articleId does not exist"))
        case Some(existing) =>
          val lang = updatedApiArticle.language

          val toUpdate = existing.copy(
            revision = Option(updatedApiArticle.revision),
            title = mergeLanguageFields(existing.title, updatedApiArticle.title.toSeq.map(t => converterService.toDomainTitle(api.ArticleTitle(t, lang)))),
            content = mergeLanguageFields(existing.content, updatedApiArticle.content.toSeq.map(c => converterService.toDomainContent(api.ArticleContentV2(c, lang)))),
            copyright = updatedApiArticle.copyright.map(c => converterService.toDomainCopyright(c)).getOrElse(existing.copyright),
            tags = mergeTags(existing.tags, converterService.toDomainTagV2(updatedApiArticle.tags, lang)),
            requiredLibraries = updatedApiArticle.requiredLibraries.map(converterService.toDomainRequiredLibraries),
            visualElement = mergeLanguageFields(existing.visualElement, updatedApiArticle.visualElement.map(c => converterService.toDomainVisualElementV2(Some(c), lang)).getOrElse(Seq())),
            introduction = mergeLanguageFields(existing.introduction, updatedApiArticle.introduction.map(i => converterService.toDomainIntroductionV2(Some(i), lang)).getOrElse(Seq())),
            metaDescription = mergeLanguageFields(existing.metaDescription, updatedApiArticle.metaDescription.map(m => converterService.toDomainMetaDescriptionV2(Some(m), lang)).getOrElse(Seq())),
            metaImageId = if (updatedApiArticle.metaImageId.isDefined) updatedApiArticle.metaImageId else existing.metaImageId,
            updated = clock.now(),
            updatedBy = authUser.userOrClientid()
          )

          contentValidator.validateArticle(toUpdate, allowUnknownLanguage = true)
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

    def allocateConceptId(externalId: Option[String], externalSubjectIds: Set[String] = Set.empty): Long = {
      val repo = conceptRepository
      externalId match {
        case None => repo.allocateConceptId
        case Some(nid) => repo.getIdFromExternalId(nid).getOrElse(repo.allocateConceptIdWithExternal(nid))
      }
    }

  }
}
