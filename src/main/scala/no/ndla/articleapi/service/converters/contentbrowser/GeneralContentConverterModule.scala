/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.model.api.ImportException
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.service.converters.HtmlTagGenerator
import no.ndla.articleapi.service.{ExtractConvertStoreContent, ExtractService, ReadService}

import scala.util.{Failure, Success, Try}

trait GeneralContentConverterModule {
  this: ExtractService with ReadService with ExtractConvertStoreContent with HtmlTagGenerator =>

  abstract class GeneralContentConverter extends ContentBrowserConverterModule with LazyLogging {
    override def convert(contentBrowser: ContentBrowser, importStatus: ImportStatus): Try[(String, Seq[RequiredLibrary], ImportStatus)] = {
      val externalId = contentBrowser.get("nid")
      val contents = extractService.getNodeGeneralContent(externalId).sortBy(c => c.language)

      contents.reverse.find(c => c.language == contentBrowser.language | c.language == Language.NoLanguage) match {
        case Some(content) =>
          insertContent(content.content, contentBrowser, importStatus) map {
            case (finalContent, status) =>
              (finalContent, Seq.empty, status)
          }
        case None =>
          Failure(ImportException(s"Failed to retrieve '$typeName' with language '${contentBrowser.language}' ($externalId)"))
      }
    }

    def insertContent(content: String, contentBrowser: ContentBrowser, importStatus: ImportStatus): Try[(String, ImportStatus)] = {
      val insertionMethod = contentBrowser.get("insertion")
      insertionMethod match {
        case "inline" => Success(content, importStatus)
        case "collapsed_body" =>
          Success(HtmlTagGenerator.buildDetailsSummaryContent(contentBrowser.get("link_text"), content), importStatus)
        case "link" => insertLink(contentBrowser, importStatus)
        case _ =>
          val warnMessage = s"""Unhandled insertion method '$insertionMethod' on '${contentBrowser.get("link_text")}'. Defaulting to link."""
          logger.warn(warnMessage)
          insertLink(contentBrowser, importStatus) map {
            case (insertString, is) =>
              (insertString, is.addMessage(warnMessage))
          }
      }
    }

    def insertLink(contentBrowser: ContentBrowser, importStatus: ImportStatus): Try[(String, ImportStatus)] = {
      getContentId(contentBrowser.get("nid"), importStatus) match {
        case Success((article: Article, is)) =>
          val embedContent = HtmlTagGenerator.buildContentLinkEmbedContent(article.id.get.toString, contentBrowser.get("link_text"))
          Success(s" $embedContent", is)

        case Success((concept: Concept, is)) =>
          val embedContent = HtmlTagGenerator.buildConceptEmbedContent(concept.id.get, contentBrowser.get("link_text"))
          Success(s" $embedContent", is)

        case Failure(e) => Failure(e)
      }
    }

    def getContentId(externalId: String, importStatus: ImportStatus): Try[(Content, ImportStatus)] = {
      readService.getContentByExternalId(externalId) match {
        case Some(content) => Success(content, importStatus.addVisitedNode(externalId))
        case None => extractConvertStoreContent.processNode(externalId, importStatus)
      }
    }

  }
}
