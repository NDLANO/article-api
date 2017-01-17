/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import java.io._
import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

import no.ndla.articleapi.model.domain.{Article, ArticleContent}
import no.ndla.articleapi.ArticleApiProperties.ArticleContentXSDSchema
import no.ndla.articleapi.model.api.{ValidationException, ValidationMessage}

import scala.util.{Failure, Try}

trait ValidationService {
  val validationService: ValidationService

  class ValidationService {
    def validateArticle(article: Article) = {
      article.content.foreach(validateArticleContent)
    }

    private def validateArticleContent(content: ArticleContent) = {
      val contentWithRootElement = s"<body>${content.content}</body>"
      validateHTML(getClass.getResourceAsStream(ArticleContentXSDSchema), new ByteArrayInputStream(contentWithRootElement.getBytes)) match {
        case Failure(e) =>
          throw new ValidationException(errors = Seq(ValidationMessage("content", e.getMessage)))
        case _ =>
      }
    }

    private[service] def validateHTML(xsdStream: InputStream, htmlFile: InputStream) = {
      Try {
        SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
          .newSchema(new StreamSource(xsdStream))
          .newValidator()
          .validate(new StreamSource(htmlFile))
      }
    }

  }
}
