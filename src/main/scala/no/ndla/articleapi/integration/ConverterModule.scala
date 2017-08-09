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
import scala.util.{Failure, Success, Try}

trait ConverterModule {
  def convert(content: LanguageContent, importStatus: ImportStatus): Try[(LanguageContent, ImportStatus)]

  def convert(nodeToConvert: NodeToConvert, importStatus: ImportStatus): Try[(NodeToConvert, ImportStatus)] = {
    @tailrec def convertLoop(contents: Seq[LanguageContent], convertedContents: Seq[LanguageContent], importStatus: ImportStatus): Try[(Seq[LanguageContent], ImportStatus)] = {
      if (contents.isEmpty) {
        Success(convertedContents, importStatus)
      } else {
        val nodeToConvert = contents.head

        convert(nodeToConvert, importStatus) match {
          case Success((content, status)) => convertLoop(contents.tail, convertedContents :+ content, status)
          case Failure(x) => Failure(x)
        }
      }
    }

    convertLoop(nodeToConvert.contents, Seq(), importStatus) map { case (convertedContent, contentImportStatus) =>
      (nodeToConvert.copy(contents=convertedContent), contentImportStatus)
    }
  }
}

object ConverterModule {
  def stringToJsoupDocument(htmlString: String): Element = {
    val document = Jsoup.parseBodyFragment(htmlString)
    document.outputSettings().escapeMode(EscapeMode.xhtml).prettyPrint(false)
    document.select("body").first()
  }

  def jsoupDocumentToString(element: Element): String = {
    element.select("body").html()
  }
}

case class LanguageContent(nid: String, tnid: String, content: String, metaDescription: String, language: String,
                           visualElement: Option[String],
                           requiredLibraries: Seq[RequiredLibrary] = List[RequiredLibrary](),
                           footNotes: Option[Map[String, FootNoteItem]] = None,
                           ingress: Option[LanguageIngress] = None) {
  def isMainNode = nid == tnid || tnid == "0"
  def isTranslation = !isMainNode

  def asContent: ArticleContent = ArticleContent(content, footNotes, language)
  def asArticleIntroduction: Option[ArticleIntroduction] = ingress.map(x => ArticleIntroduction(x.content, language))
  def asArticleMetaDescription: ArticleMetaDescription = ArticleMetaDescription(metaDescription, language)
  def asVisualElement: Option[VisualElement] = visualElement.map(visual => VisualElement(visual, language))
}

case class LanguageIngress(content: String, ingressImage: Option[String])
