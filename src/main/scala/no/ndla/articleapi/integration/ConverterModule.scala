/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.integration

import no.ndla.articleapi.model.domain._
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities.EscapeMode
import scala.annotation.tailrec

trait ConverterModule {
  def stringToJsoupDocument(htmlString: String): Element = {
    val document = Jsoup.parseBodyFragment(htmlString)
    document.outputSettings().escapeMode(EscapeMode.xhtml).prettyPrint(false)
    document.select("body").first()
  }

  def jsoupDocumentToString(element: Element): String = {
    element.select("body").html()
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

    val (convertedContent, contentImportStatus) = convertLoop(nodeToConvert.contents, Seq(), importStatus)


    (nodeToConvert.copy(contents=convertedContent), contentImportStatus)
  }
}

case class LanguageContent(nid: String, tnid: String, content: String, language: Option[String],
                           requiredLibraries: Seq[RequiredLibrary] = List[RequiredLibrary](),
                           footNotes: Option[Map[String, FootNoteItem]] = None,
                           ingress: Option[LanguageIngress] = None) {
  def isMainNode = nid == tnid || tnid == "0"
  def isTranslation = !isMainNode

  def asContent: ArticleContent = ArticleContent(content, footNotes, language)
  def asArticleIntroduction: Option[ArticleIntroduction] = ingress.map(x => ArticleIntroduction(x.content, language))
}

case class LanguageIngress(content: String)
