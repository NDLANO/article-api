/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import scala.util.matching.Regex

case class ContentBrowser(textContainingContentBrowser: String, language: String) {
  // Extract the contentbrowser variables
  private val Pattern: Regex = """(?s).*(\[contentbrowser (.*) ?contentbrowser(?:_margin_left|_margin_right)?\]).*""".r
  val (contentBrowser, contentBrowserData) = textContainingContentBrowser match {
    case Pattern(contentBrowserString, contentBrowserStringData) => (contentBrowserString, contentBrowserStringData)
    case _ => ("", "")
  }

  private val contentBrowserWithoutBrackets = IsContentBrowserField match {
    case true => contentBrowser.substring(1, contentBrowser.length - 1)
    case false => contentBrowser
  }

  // Extract every key-value pair and build a map
  private val KeyVal = contentBrowserWithoutBrackets.split("==").map(x => x.stripPrefix("=").split("="))
  private val FieldMap = KeyVal.map(el => el(0) -> (if (el.length > 1) el(1) else "")).toMap

  lazy val IsContentBrowserField: Boolean = textContainingContentBrowser.matches(Pattern.toString)

  lazy val StartEndIndex: (Int, Int) = {
    val startIndex = textContainingContentBrowser.indexOf(contentBrowser)
    (startIndex, startIndex + contentBrowser.length)
  }

  def get(key: String): String = {
    FieldMap.getOrElse(key, "")
  }
}
