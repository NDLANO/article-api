/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.ArticleApiProperties._
import no.ndla.articleapi.model.domain.{ImportStatus, RequiredLibrary}
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.service.converters.HtmlTagGenerator
import no.ndla.articleapi.service.{ExtractConvertStoreContent, ExtractService}

import scala.util.{Failure, Success}

trait GeneralContentConverterModule {
  this: ExtractService with ArticleRepository with ExtractConvertStoreContent =>

  abstract class GeneralContentConverter extends ContentBrowserConverterModule with LazyLogging {
    override def convert(contentBrowser: ContentBrowser, visitedNodes: Seq[String]): (String, Seq[RequiredLibrary], ImportStatus) = {
      val externalId = contentBrowser.get("nid")
      val contents = extractService.getNodeGeneralContent(externalId)

      contents.find(x => x.language == contentBrowser.language.getOrElse("")) match {
        case Some(content) => {
          val (finalContent, status) = insertContent(content.content, contentBrowser, visitedNodes)
          (finalContent, Seq(), status)
        }
        case None => {
          val errorMsg = s"Failed to retrieve '$typeName' with language '${contentBrowser.language.getOrElse("")}' ($externalId)"
          logger.warn(errorMsg)
          (s"{Import error: $errorMsg}", Seq(), ImportStatus(List(errorMsg), visitedNodes))
        }
      }
    }

    def insertContent(content: String, contentBrowser: ContentBrowser, visitedNodes: Seq[String]): (String, ImportStatus) = {
      val insertionMethod = contentBrowser.get("insertion")
      insertionMethod match {
        case "inline" => (content, ImportStatus(Seq(), visitedNodes))
        case "collapsed_body" => (s"<details><summary>${contentBrowser.get("link_text")}</summary>$content</details>", ImportStatus(Seq(), visitedNodes))
        case "link" => insertLink(content, contentBrowser, visitedNodes)
        case _ => {
          val warnMessage = s"""Unhandled insertion method '$insertionMethod' on '${contentBrowser.get("link_text")}'. Defaulting to link."""
          logger.warn(warnMessage)
          val (insertString, importStatus) = insertLink(content, contentBrowser, visitedNodes)
          (insertString, ImportStatus(importStatus.messages :+ warnMessage, importStatus.visitedNodes))
        }
      }
    }

    def insertLink(content: String, contentBrowser: ContentBrowser, visitedNodes: Seq[String]): (String, ImportStatus) = {
      val (contentId, importStatus) = getContentId(contentBrowser.get("nid"), visitedNodes)

      contentId match {
        case Some(id) => {
          val (embedContent, embedUsageErrors) = HtmlTagGenerator.buildEmbedContent(Map(
            "resource" -> "content-link",
            "id" -> s"${contentBrowser.id}",
            "content-id" -> id.toString,
            "link-text" -> contentBrowser.get("link_text")
          ))
          (s" $embedContent", importStatus ++ ImportStatus(embedUsageErrors, Seq()))
        }
        case None => {
          val warnMessage = s"""Link to old ndla.no ($ndlaBaseHost/node/${contentBrowser.get("nid")})"""
          logger.warn(warnMessage)

          val href = s"$ndlaBaseHost/node/${contentBrowser.get("nid")}"
          val linkText = contentBrowser.get("link_text")
          val (anchor, usageErrors) = HtmlTagGenerator.buildAnchor(href, linkText)

          (s" $anchor", importStatus.copy(messages=importStatus.messages ++ usageErrors ++ Seq(warnMessage)))
        }
      }
    }

    def getContentId(externalId: String, visitedNodes: Seq[String]): (Option[Long], ImportStatus) = {
      articleRepository.getIdFromExternalId(externalId) match {
        case Some(id) => (Some(id), ImportStatus(Seq(), (visitedNodes :+ externalId).distinct))
        case None => {
          extractConvertStoreContent.processNode(externalId, ImportStatus(Seq(), visitedNodes)) match {
            case Success((newId, importStatus)) => (Some(newId), importStatus)
            case Failure(exc) => (None, ImportStatus(Seq(exc.getMessage), visitedNodes))
          }
        }
      }
    }

  }
}
