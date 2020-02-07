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
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.service.search.ArticleIndexService
import no.ndla.articleapi.validation.ContentValidator

import scala.util.{Failure, Try}

trait WriteService {
  this: ArticleRepository
    with ConverterService
    with ContentValidator
    with ArticleIndexService
    with Clock
    with User
    with ReadService
    with ArticleIndexService =>
  val writeService: WriteService

  class WriteService {

    def updateArticle(article: Article, externalIds: List[Long], useImportValidation: Boolean): Try[Article] = {
      for {
        _ <- contentValidator.validateArticle(article,
                                              allowUnknownLanguage = true,
                                              isImported = externalIds.nonEmpty || useImportValidation)
        domainArticle <- articleRepository.updateArticleFromDraftApi(article, externalIds.map(_.toString))
        _ <- articleIndexService.indexDocument(domainArticle)
      } yield domainArticle
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

  }
}
