package no.ndla.contentapi.integration

import no.ndla.contentapi.model.{Content, ContentInformation, ImportStatus, RequiredLibrary}
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities.EscapeMode

import scala.collection.mutable.ListBuffer

trait ConverterModule {
  var importStatus: ImportStatus = ImportStatus(List[String]())
  var requiredLibraries: List[RequiredLibrary] = List[RequiredLibrary]()

  def stringToJsoupDocument(htmlString: String): Element = {
    val document = Jsoup.parseBodyFragment(htmlString)
    document.outputSettings().escapeMode(EscapeMode.xhtml)
    val article = document.select("article")
    val content = article.isEmpty() match {
      case false => article
      case true => document.select("body")
    }
    content.first()
  }

  def jsoupDocumentToString(element: Element): String = {
    val article = element.select("article")
    val content = article.isEmpty() match {
      case false => article
      case true => element.select("body")
    }

    content.outerHtml()
  }

  def convert(content: Content): Content

  def reset() = {
    importStatus = ImportStatus(List[String]())
    requiredLibraries = List[RequiredLibrary]()
  }

  def convert(contentInformation: ContentInformation): (ContentInformation, ImportStatus) = {
    reset()
    val content = contentInformation.content.map(convert)
    (contentInformation.copy(content=content, requiredLibraries=requiredLibraries.map(x => x.copy())), importStatus.copy())
  }
}
