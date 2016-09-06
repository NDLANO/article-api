/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.integration

import no.ndla.articleapi.model._
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities.EscapeMode

import scala.annotation.tailrec


trait ConverterModule {
  def stringToJsoupDocument(htmlString: String): Element = {
    val document = Jsoup.parseBodyFragment(htmlString)
    document.outputSettings().escapeMode(EscapeMode.xhtml)
    val article = document.select("article")
    val content = article.isEmpty match {
      case false => article
      case true => document.select("body")
    }
    content.first()
  }

  def jsoupDocumentToString(element: Element): String = {
    val article = element.select("article")
    val content = article.isEmpty match {
      case false => article
      case true => element.select("body")
    }

    content.outerHtml()
  }

  def convert(content: LanguageContent, importStatus: ImportStatus): (LanguageContent, ImportStatus)

  def convert(nodeToConvert: NodeToConvert, importStatus: ImportStatus): (NodeToConvert, ImportStatus) = {
    @tailrec def convertLoop(contents: Seq[LanguageContent], convertedContents: Seq[LanguageContent], importStatus: ImportStatus): (Seq[LanguageContent], ImportStatus) = {
      if (contents.isEmpty) {
        (convertedContents, importStatus)
      } else {
        val nodeToConvert = contents.head
        val (content, status) = convert(nodeToConvert, importStatus)

        convertLoop(contents.tail, convertedContents :+ content, status)
      }
    }

    val (convertedContent, finalImportStatus) = convertLoop(nodeToConvert.contents, Seq(), importStatus)
    (nodeToConvert.copy(contents=convertedContent), finalImportStatus)
  }

}

case class LanguageContent(nid: String, tnid: String, content: String, language: Option[String], ingress: Option[NodeIngress],
                           requiredLibraries: Seq[RequiredLibrary] = List[RequiredLibrary](),
                           containsIngress: Boolean = false,
                           footNotes: Option[Map[String, FootNoteItem]] = None) {
  def isMainNode = nid == tnid || tnid == "0"
  def isTranslation = !isMainNode

  def asContent: Article = Article(content, footNotes, language)
}
