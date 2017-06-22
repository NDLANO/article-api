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

import scala.collection.JavaConversions._
import scala.util.{Success, Try}

object SimpleTagConverter extends ConverterModule {

  def convert(content: LanguageContent, importStatus: ImportStatus): Try[(LanguageContent, ImportStatus)] = {
    val element = stringToJsoupDocument(content.content)
    convertDivs(element)
    convertPres(element)
    Success(content.copy(content = jsoupDocumentToString(element)), importStatus)
  }

  def convertDivs(el: Element) {
    for (el <- el.select("div")) {
      el.className() match {
        case "right" => replaceTag(el, "aside")
        case "paragraph" => replaceTag(el, "section")
        case "quote" => replaceTag(el, "blockquote")
        case "hide" => handle_hide(el)
        case "frame" => {
          el.removeClass("frame")
          el.addClass("c-bodybox")
        }
        case "full" | "wrapicon" | "no_icon" => el.unwrap()
        case _ =>
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

  private def convertPres(el: Element) {
    for (el <- el.select("pre")) {
      el.html("<code>" + el.html() + "</code>")
    }
  }
}
