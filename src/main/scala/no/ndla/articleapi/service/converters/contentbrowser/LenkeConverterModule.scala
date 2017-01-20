/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import com.netaporter.uri.dsl._
import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.integration.MigrationEmbedMeta
import no.ndla.articleapi.model.domain.{ImportStatus, RequiredLibrary}
import no.ndla.articleapi.service.ExtractService
import no.ndla.articleapi.service.converters.{Attributes, HtmlTagGenerator}
import org.jsoup.Jsoup

trait LenkeConverterModule {
  this: ExtractService with HtmlTagGenerator =>

  object LenkeConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "lenke"

    override def convert(content: ContentBrowser, visitedNodes: Seq[String]): (String, Seq[RequiredLibrary], ImportStatus) = {
      logger.info(s"Converting lenke with nid ${content.get("nid")}")
      val (replacement, requiredLibraries, errors) = convertLink(content)
      (replacement, requiredLibraries, ImportStatus(errors, visitedNodes))
    }

    def convertLink(cont: ContentBrowser): (String, Seq[RequiredLibrary], Seq[String]) = {
      val embedMeta = extractService.getNodeEmbedMeta(cont.get("nid")) match {
        case Some(meta) => meta
        case _ => {
          val message = s"Failed to import embed meta (${cont.get("nid")})"
          logger.warn(message)
          return ("", Seq(), message :: Nil)
        }
      }

      val (url, embedCode) = (embedMeta.url.getOrElse(""), embedMeta.embedCode.getOrElse(""))
      val (htmlTag, requiredLibrary, errors) = cont.get("insertion") match {
        case "link" => insertAnchor(url, cont)
        case "inline" => insertInline(url, embedCode, cont)
        case "lightbox_large" => insertAnchor(url, cont)
        case "collapsed_body" => insertDetailSummary(url, embedCode, cont)
        case _ => insertUnhandled(url, cont)
      }

      val NDLAPattern = """.*(ndla.no).*""".r
      url.host.getOrElse("") match {
        case NDLAPattern(_) => {
          logger.warn("Link to NDLA resource: '{}'", url)
          (htmlTag, requiredLibrary.toList, errors :+ s"(Warning) Link to NDLA resource '$url'")
        }
        case _ => (htmlTag, requiredLibrary.toList, errors)
      }
    }

    private def insertInline(url: String, embedCode: String, cont: ContentBrowser): (String, Option[RequiredLibrary], Seq[String]) = {
      val message = s"External resource to be embedded: $url"
      logger.info(message)

      val (embedTag, requiredLibs) = isNRKLink(url) match {
        case false => (HtmlTagGenerator.buildExternalInlineEmbedContent(url), None)
        case true => getNrkEmbedTag(embedCode, url)
      }
      (embedTag, requiredLibs, message :: Nil)
    }

    private def isNRKLink(url: String): Boolean = {
      val NRKUrlPattern = """(.*nrk.no)""".r
      url.host.getOrElse("") match {
        case NRKUrlPattern(_) => true
        case _ => false
      }
    }

    def getNrkEmbedTag(embedCode: String, url: String): (String, Option[RequiredLibrary]) = {
      val doc = Jsoup.parseBodyFragment(embedCode)
      val (videoId, requiredLibraryUrl) = (doc.select("div[data-nrk-id]").attr("data-nrk-id"), doc.select("script").attr("src"))
      val requiredLibrary = RequiredLibrary("text/javascript", "NRK video embed", requiredLibraryUrl)
      (HtmlTagGenerator.buildNRKInlineVideoContent(videoId, url), Some(requiredLibrary))
    }

    private def insertDetailSummary(url: String, embedCode: String, cont: ContentBrowser): (String, Option[RequiredLibrary], Seq[String]) = {
      val (elementToInsert, requiredLib, figureErrors) = insertInline(url, embedCode, cont)
      (s"<details><summary>${cont.get("link_text")}</summary>$elementToInsert</details>", requiredLib, figureErrors)
    }

    private def insertAnchor(url: String, cont: ContentBrowser): (String, Option[RequiredLibrary], Seq[String]) = {
      val htmlTag = HtmlTagGenerator.buildAnchor(url, cont.get("link_text"), cont.get("link_title_text"))
      (s" $htmlTag", None, Seq())
    }

    private def insertUnhandled(url: String, cont: ContentBrowser): (String, Option[RequiredLibrary], Seq[String]) = {
      val (anchor, requiredLib, anchorErrors) = insertAnchor(url, cont)
      val message = s"""Unhandled insertion method '${cont.get("insertion")}' on '${cont.get("link_text")}'. Defaulting to link."""

      logger.warn(message)
      (anchor, requiredLib, anchorErrors :+ message)
    }
  }

}
