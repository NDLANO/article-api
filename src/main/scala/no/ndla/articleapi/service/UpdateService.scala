/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import no.ndla.articleapi.model.api
import no.ndla.articleapi.repository.ArticleRepository


trait UpdateService {
  this: ArticleRepository with ConverterService =>
  val updateService: UpdateService

  class UpdateService {
    def newArticle(newArticle: api.NewArticle) = {
      val domainArticle = converterService.toDomainArticle(newArticle)
      ValidationService.validateArticle(domainArticle)
      converterService.toApiArticle(articleRepository.insert(domainArticle))
    }

  }
}
