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
import javax.xml.parsers.SAXParserFactory
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

import org.xml.sax.InputSource

import scala.xml.parsing.NoBindingFactoryAdapter
import scala.xml.{Elem, TopScope}
import javax.xml.parsers.{SAXParser, SAXParserFactory}
import javax.xml.validation.Schema

import no.ndla.articleapi.model.domain.{Article, ArticleContent}
import no.ndla.articleapi.ArticleApiProperties.ArticleContentXSDSchema
import no.ndla.articleapi.model.api.ValidationException

import scala.util.{Failure, Try}

object ValidationService {
  def validateArticle(article: Article) = {
    article.content.foreach(validateArticleContent)
  }

  private def validateArticleContent(content: ArticleContent) = {
    val contentWithRootElement = s"<body>${content.content}</body>"
    validateHTML(ArticleContentXSDSchema, new ByteArrayInputStream(contentWithRootElement.getBytes))
  }

  private[service] def validateHTML(xsdFile: String, htmlFile: InputStream) = {
    Try {
      val factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
      val xsdStream = getClass.getResourceAsStream(xsdFile)
      val schema = factory.newSchema(new StreamSource(xsdStream))
      val xml = new SchemaAwareFactoryAdapter(schema).load(htmlFile)
    } match {
      case Failure(e) =>
        throw new ValidationException(e.getMessage)
      case _ =>
    }
  }

  private[service] def generateTempFile(data: String, filetype: String): File = {
    val temp = File.createTempFile("pattern", filetype)
    val out = new BufferedWriter(new FileWriter(temp))
    out.write(data)
    out.close()

    temp.deleteOnExit()
    temp
  }

  // copy-paste: http://sean8223.blogspot.no/2009/09/xsd-validation-in-scala.html
  private class SchemaAwareFactoryAdapter(schema: Schema) extends NoBindingFactoryAdapter {
    override def loadXML(source: InputSource, parser: SAXParser) = {
      val reader = parser.getXMLReader
      val handler = schema.newValidatorHandler()
      handler.setContentHandler(this)
      reader.setContentHandler(handler)

      scopeStack.push(TopScope)
      reader.parse(source)
      scopeStack.pop
      rootElem.asInstanceOf[Elem]
    }

    override def parser: SAXParser = {
      val factory = SAXParserFactory.newInstance()
      factory.setNamespaceAware(true)
      factory.setFeature("http://xml.org/sax/features/namespace-prefixes", true)
      factory.newSAXParser()
    }
  }

}
