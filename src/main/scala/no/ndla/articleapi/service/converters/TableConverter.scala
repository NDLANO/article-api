/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.converters

import no.ndla.articleapi.integration.{ConverterModule, LanguageContent}
import no.ndla.articleapi.model.ImportStatus
import org.jsoup.nodes.Element
import scala.collection.JavaConversions._

object TableConverter extends ConverterModule {
  override def convert(content: LanguageContent, importStatus: ImportStatus): (LanguageContent, ImportStatus) = {
    val element = stringToJsoupDocument(content.content)

    stripParagraphTag(element)
    convertFirstTrToTh(element)

    (content.copy(content=jsoupDocumentToString(element)), importStatus)
  }

  def stripParagraphTag(el: Element) = {
    for (cell <- el.select("td")) {
      val paragraphs = cell.select("p")
      if (paragraphs.size() == 1) {
        paragraphs.first.unwrap
      }
    }
  }

  def convertFirstTrToTh(el: Element) = {
    for (table <- el.select("table")) {
      Option(table.select("tr").first).foreach(firstRow => {
        firstRow.select("td").tagName("th")
        firstRow.select("strong").unwrap()
      })
    }
  }

}
