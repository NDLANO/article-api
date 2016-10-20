/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import no.ndla.articleapi.model.api.Article
import no.ndla.articleapi.repository.ArticleRepositoryComponent

trait ReadService {
  this: ArticleRepositoryComponent with ConverterServiceComponent =>
  val readService: ReadService

  class ReadService {
    def withId(id: Long): Option[Article] =
      articleRepository.withId(id).map(converterService.toApiArticle)

  }

}
