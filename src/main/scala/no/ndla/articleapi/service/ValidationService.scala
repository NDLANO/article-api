/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import java.io.{BufferedWriter, File, FileWriter}
import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

import no.ndla.articleapi.model.domain.{Article, ArticleContent}
import no.ndla.articleapi.ArticleApiProperties.ArticleContentXSDSchema
import no.ndla.articleapi.model.api.ValidationException

import scala.util.{Failure, Try}

object ValidationService {
  def validateArticle(article: Article) = {
    article.content.foreach(validateArticleContent)
  }

  def validateArticleContent(content: ArticleContent) = {
    validateHTML(new File(ArticleContentXSDSchema), generateTempFile(content.content, ".xml"))
  }

  def validateHTML(xsdFile: File, htmlFile: File) = {
    Try {
      val factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
      val schema = factory.newSchema(xsdFile)
      val validator = schema.newValidator()
      validator.validate(new StreamSource(htmlFile))
    } match {
      case _ =>
      case Failure(e) => throw new ValidationException(e.getMessage)
    }
  }

  private[service] def generateTempFile(data: String, filetype: String): File = {
    val temp = File.createTempFile("pattern", filetype)
    temp.deleteOnExit()
    val out = new BufferedWriter(new FileWriter(temp))
    out.write(data)
    out.close()
    temp
  }
}
