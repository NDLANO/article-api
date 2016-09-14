/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.model

import java.net.URL
import java.util.Date

import com.netaporter.uri.dsl._
import no.ndla.articleapi.integration.{LanguageContent, MigrationRelatedContents}

case class NodeGeneralContent(nid: String, tnid: String, title: String, content: String, language: String) {
  def isMainNode = (nid == tnid || tnid == "0")
  def isTranslation = !isMainNode

  def asContentTitle = ArticleTitle(title, Some(language))
}

case class NodeToConvert(titles: Seq[ArticleTitle], contents: Seq[LanguageContent], copyright: Copyright, tags: Seq[ArticleTag],
                         visualElements: Seq[VisualElement], ingress: Seq[NodeIngress], contentType: String, created: Date, updated: Date)

case class ContentFilMeta(nid: String, tnid: String, title: String, fileName: String, url: URL, mimeType: String, fileSize: String)
object ContentFilMeta {
  implicit def stringToUrl(s: String): URL = new URL(s.uri)
}

case class NodeIngress(nid: String, tnid: String, content: String, imageNid: Option[String], ingressVisPaaSiden: Int, language: Option[String]) {
  def asArticleIngress: ArticleIntroduction = ArticleIntroduction(content, imageNid, ingressVisPaaSiden == 1, language)

  def asLanguageContent: LanguageContent = LanguageContent(nid, tnid, content, language)
}

case class BiblioMeta(biblio: Biblio, authors: Seq[BiblioAuthor])
case class Biblio(title: String, bibType: String, year: String, edition: String, publisher: String)
case class BiblioAuthor(name: String, lastname: String, firstname: String)
