package no.ndla.contentapi.integration

import no.ndla.contentapi.model.{Content, ContentInformation, ImportStatus, RequiredLibrary}
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities.EscapeMode


trait ConverterModule {
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

  def convert(content: LanguageContent): (LanguageContent, ImportStatus)

  def convert(contentInformation: ContentInformation, importStatus: ImportStatus): (ContentInformation, ImportStatus) = {
    val languageContent = contentInformation.content.map(LanguageContent(_))
    val (convertedContent, importStatuses) = languageContent.map(x => convert(x)).unzip

    val content = convertedContent.map(content => content.asContent)
    val requiredLibraries = convertedContent.flatMap(content => content.requiredLibraries) // Slå sammen requiredLibraries
    val finalImportStatuses = ImportStatus(importStatuses.flatMap(is => is.messages) ++ importStatus.messages)  // Slå sammen importStatus

    (contentInformation.copy(content=content, requiredLibraries=requiredLibraries.distinct), finalImportStatuses)
  }
}

object LanguageContent {
  def apply(arg: Content): LanguageContent = LanguageContent(arg.content, arg.language)
}

case class LanguageContent(content: String, language: Option[String], requiredLibraries: Seq[RequiredLibrary] = List[RequiredLibrary]()) {
  def asContent(): Content = Content(content, language)
}
