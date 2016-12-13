/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import scala.util.matching.Regex

case class ContentBrowser(textContainingContentBrowser: String, language: Option[String]) {
  // Extract the contentbrowser variables
  private val Pattern: Regex = """(?s).*(\[contentbrowser (.*) ?contentbrowser(?:_margin_left)?\]).*""".r
  val (contentBrowser, contentBrowserData) = textContainingContentBrowser match {
    case Pattern(contentBrowserString, contentBrowserStringData) => (contentBrowserString, contentBrowserStringData)
    case _ => ("", "")
  }
  // Extract every key-value pair and build a map
  private val KeyVal = contentBrowserData.split("==").map(x => x.stripPrefix("=").split("="))
  private val FieldMap = KeyVal.map(el => el(0) -> (if (el.length > 1) el(1) else "")).toMap

  def isContentBrowserField(): Boolean = {
    textContainingContentBrowser.matches(Pattern.toString)
  }

  def getStartEndIndex(): (Int, Int) = {
    val startIndex = textContainingContentBrowser.indexOf(contentBrowser)
    (startIndex, startIndex + contentBrowser.length)
  }

  def get(key: String): String = {
    FieldMap.getOrElse(key, "")
  }
}
