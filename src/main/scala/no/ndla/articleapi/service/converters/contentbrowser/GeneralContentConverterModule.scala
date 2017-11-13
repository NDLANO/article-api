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
      val externalId = contentBrowser.get("nid")

      getContentId(externalId, importStatus) match {
        case Success((Some(articleId), _, is)) =>
          val embedContent = HtmlTagGenerator.buildContentLinkEmbedContent(articleId, contentBrowser.get("link_text"))
          Success(s" $embedContent", is)
        case Success((_, Some(conceptId), is)) =>
          val embedContent = HtmlTagGenerator.buildConceptEmbedContent(conceptId, contentBrowser.get("link_text"))
          Success(s" $embedContent", is)
        case Success((None, None, _)) => Failure(ImportException(s"Failed to retrieve or import article with external id $externalId"))
        case Failure(e) => Failure(e)
      }
    }

    private def getContentId(externalId: String, importStatus: ImportStatus): Try[(Option[Long], Option[Long], ImportStatus)] = {
      val mainNodeId = extractConvertStoreContent.getMainNodeId(externalId).getOrElse(externalId)

      (readService.getArticleIdByExternalId(mainNodeId), readService.getConceptIdByExternalId(mainNodeId)) match {
        case (None, None) =>
          logger.info(s"Article with node id $mainNodeId does not exist. Importing it!")
          extractConvertStoreContent.processNode(mainNodeId, importStatus) match {
            case Success((c: Article, is)) => Success(c.id, None, is)
            case Success((c: Concept, is)) => Success(None, c.id, is)
            case Failure(ex) => Failure(ex)
          }
        case (Some(articleId), _) => Success(Some(articleId), None, importStatus)
        case (None, Some(conceptid)) => Success(None, Some(conceptid), importStatus)
      }
    }

  }
}
