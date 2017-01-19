/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import no.ndla.articleapi.integration.ConverterModule.stringToJsoupDocument
import no.ndla.articleapi.model.api.{ValidationException, ValidationMessage}
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.service.converters.HTMLCleaner

import scala.collection.JavaConversions._

trait ValidationService {
  val validationService: ValidationService

  class ValidationService {
    def validateArticle(article: Article) = {
      val validationErrors = article.content.flatMap(validateContent) ++
        article.introduction.flatMap(validateIntroduction) ++
        article.metaDescription.flatMap(validateMetaDescription) ++
        article.title.flatMap(validateTitle)

      if (validationErrors.nonEmpty)
        throw new ValidationException(errors=validationErrors)
    }

    def validateContent(content: ArticleContent) = {
      getIllegalTags(content.content)
        .map(illegalTag => ValidationMessage("content", s"Article contains illegal tag $illegalTag")).toSeq
    }

    def validateIntroduction(content: ArticleIntroduction): Option[ValidationMessage] = {
      getTags(content.introduction).headOption.map(_ => ValidationMessage("metaDescription", "Meta introduction can not include HTML tags"))
    }

    def validateMetaDescription(content: ArticleMetaDescription): Option[ValidationMessage] = {
      getTags(content.content).headOption.map(_ => ValidationMessage("metaDescription", "Meta introduction can not include HTML tags"))
    }

    def validateTitle(content: ArticleTitle): Option[ValidationMessage] = {
      getTags(content.title).headOption.map(_ => ValidationMessage("title", "Meta introduction can not include HTML tags"))
    }

    private def getTags(content: String) = {
      stringToJsoupDocument(content).children.select("*").map(_.tagName)
    }

    private def getIllegalTags(content: String) = {
        getTags(content).filter(htmlTag => !HTMLCleaner.isTagValid(htmlTag)).toSet
    }

  }
}
