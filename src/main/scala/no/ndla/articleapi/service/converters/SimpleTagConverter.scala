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
import org.jsoup.nodes.Element

import scala.collection.JavaConverters._
import scala.util.{Success, Try}

object SimpleTagConverter extends ConverterModule {

  def convert(content: LanguageContent, importStatus: ImportStatus): Try[(LanguageContent, ImportStatus)] = {
    val element = stringToJsoupDocument(content.content)
    convertDivs(element)
    convertHeadings(element)
    convertPres(element)
    Success(content.copy(content = jsoupDocumentToString(element)), importStatus)
  }

  def convertDivs(el: Element) {
    for (el <- el.select("div").asScala) {
      el.className() match {
        case "right" => replaceTag(el, "aside")
        case "paragraph" => replaceTag(el, "section")
        case "quote" => replaceTag(el, "blockquote")
        case "hide" => handle_hide(el)
        case "frame" =>
          el.removeClass("frame")
          el.addClass("c-bodybox")
        case "full" | "wrapicon" | "no_icon" => el.unwrap()
        case cellContent if cellContent contains "ndla_table_cell_content" => el.unwrap()
        case cell if cell contains "ndla_table_cell" => replaceTag(el, "td")
        case row if row contains "ndla_table_row" => replaceTag(el, "tr")
        case table if table contains "ndla_table" => replaceTag(el, "table")
        case _ => el.removeAttr("class")
      }
    }
  }

  def convertHeadings(el: Element) {
    for (el <- el.select("h1, h2, h3, h4, h5, h6").asScala) {
      el.className() match {
        case "frame" => replaceTagWithClass(el, "div", "c-bodybox")
        case _ => el
      }
    }
  }


  private def handle_hide(el: Element) {
    replaceTag(el, "details")
    el.select("a.re-collapse").remove()
    val details = el.select("div.details").html() // save content
    el.select("div.details").remove()
    val summary = el.text()
    el.html(s"<summary>$summary</summary>")
    el.append(details)
  }

  private def replaceTag(el: Element, replacementTag: String) {
    el.tagName(replacementTag)
    el.removeAttr("class")
  }


  private def replaceTagWithClass(el: Element, replacementTag: String, className: String) {
    replaceTag(el, replacementTag)
    el.addClass(className)
  }


  private def convertPres(el: Element) {
    for (el <- el.select("pre").asScala) {
      el.html("<code>" + el.html() + "</code>")
    }
  }
}
