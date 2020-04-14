/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.auth.User
import no.ndla.articleapi.model.api
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.service.search.ArticleIndexService
import no.ndla.articleapi.validation.ContentValidator
import no.ndla.articleapi.integration.SearchApiClient
import no.ndla.validation.ValidationException

import scala.util.{Failure, Success, Try}

trait WriteService {
  this: ArticleRepository
    with ConverterService
    with ContentValidator
    with ArticleIndexService
    with Clock
    with User
    with ReadService
    with ArticleIndexService
    with SearchApiClient =>
  val writeService: WriteService

  class WriteService extends LazyLogging {

    def updateArticle(
        article: Article,
        externalIds: List[Long],
        useImportValidation: Boolean,
        useSoftValidation: Boolean
    ): Try[Article] = {

      val strictValidationResult = contentValidator.validateArticle(
        article,
        allowUnknownLanguage = true,
        isImported = externalIds.nonEmpty || useImportValidation
      )

      val validationResult =
        if (useSoftValidation) {
          (strictValidationResult, contentValidator.softValidateArticle(article)) match {
            case (Failure(strictEx: ValidationException), Success(art)) =>
              val strictErrors = strictEx.errors
                .map(msg => {
                  s"\t'${msg.field}' => '${msg.message}'"
                })
                .mkString("\n\t")

              logger.warn(
                s"Article with id '${art.id.getOrElse(-1)}' was updated with soft validation while strict validation failed with the following errors:\n$strictErrors")
              Success(art)
            case (_, Success(art)) => Success(art)
            case (_, Failure(ex))  => Failure(ex)
          }
        } else strictValidationResult

      for {
        _ <- validationResult
        domainArticle <- articleRepository.updateArticleFromDraftApi(article, externalIds.map(_.toString))
        _ <- articleIndexService.indexDocument(domainArticle)
        _ <- Try(searchApiClient.indexArticle(domainArticle))
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
