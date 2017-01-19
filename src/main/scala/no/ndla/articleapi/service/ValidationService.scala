/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import no.ndla.articleapi.model.api.{ValidationException, ValidationMessage}
import no.ndla.articleapi.model.domain.{Article, ArticleContent}
import no.ndla.articleapi.service.converters.HTMLCleaner
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities.EscapeMode

import scala.collection.JavaConversions._

trait ValidationService {
  val validationService: ValidationService

  class ValidationService {
    def validateArticle(article: Article) = {
      val validationErrors = article.content.flatMap(validateContent)
      if (validationErrors.nonEmpty)
        throw new ValidationException(errors=validationErrors)
    }

    def validateContent(content: ArticleContent) = {
      val illegalTags = getIllegalTags(stringToJsoupDocument(content.content))
      illegalTags.map(illegalTag => ValidationMessage("content", s"Article contains illegal tag $illegalTag")).toSeq
    }

    def stringToJsoupDocument(htmlString: String): Element = {
      val document = Jsoup.parseBodyFragment(htmlString)
      document.outputSettings().escapeMode(EscapeMode.xhtml).prettyPrint(false)
      document.select("body").first()
    }

    private def getIllegalTags(el: Element) = {
      el.children().select("*").map(_.tagName)
        .filter(htmlTag => !HTMLCleaner.isTagValid(htmlTag)).toSet
    }

  }
}
