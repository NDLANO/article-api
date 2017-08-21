/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters

import no.ndla.articleapi.integration.ConverterModule.{jsoupDocumentToString, stringToJsoupDocument}
import no.ndla.articleapi.integration.{ConverterModule, LanguageContent}
import no.ndla.articleapi.model.domain.ImportStatus
import no.ndla.articleapi.service.converters.Attributes.XMLNsAttribute
import org.jsoup.nodes.Element

import scala.collection.JavaConverters._
import scala.util.{Success, Try}

object MathMLConverter extends ConverterModule {

  def convert(content: LanguageContent, importStatus: ImportStatus): Try[(LanguageContent, ImportStatus)] = {
    val element = stringToJsoupDocument(content.content)
    addMathMlAttributes(element)
    replaceNbsp(element)

    Success(content.copy(content=jsoupDocumentToString(element)), importStatus)
  }

  def addMathMlAttributes(el: Element) = {
    el.select("math").asScala.foreach(e => e.attr(s"$XMLNsAttribute", "http://www.w3.org/1998/Math/MathML"))
  }

  def replaceNbsp(el: Element) = el.html(el.html().replace(NBSP, " "))
}
