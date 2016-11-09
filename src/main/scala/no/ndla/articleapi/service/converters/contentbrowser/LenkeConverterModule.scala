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
import no.ndla.articleapi.model.domain.{ImportStatus, RequiredLibrary}
import no.ndla.articleapi.service.ExtractServiceComponent
import no.ndla.articleapi.service.converters.HtmlTagGenerator
import org.jsoup.Jsoup

trait LenkeConverterModule {
  this: ExtractServiceComponent =>

  object LenkeConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "lenke"

    override def convert(content: ContentBrowser, visitedNodes: Seq[String]): (String, Seq[RequiredLibrary], ImportStatus) = {
      logger.info(s"Converting lenke with nid ${content.get("nid")}")
      val (replacement, requiredLibraries, errors) = convertLink(content)
      (replacement, requiredLibraries, ImportStatus(errors, visitedNodes))
    }

    def convertLink(cont: ContentBrowser): (String, Seq[RequiredLibrary], Seq[String]) = {
      val url = extractService.getNodeEmbedUrl(cont.get("nid")).get
      val (htmlTag, requiredLibrary, errors) = cont.get("insertion") match {
        case "link" => insertAnchor(url, cont)
        case "inline" => insertInline(url, cont)
        case "lightbox_large" => insertAnchor(url, cont)
        case "collapsed_body" => insertDetailSummary(url, cont)
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

    private def insertInline(url: String, cont: ContentBrowser): (String, Option[RequiredLibrary], Seq[String]) = {
      val message = s"External resource to be embedded: $url"
      val attributes = Map("resource" -> "external", "id" -> s"${cont.id}", "url" -> url)

      logger.info(message)
      val (extraAttributes, requiredLibs) = getExtraAttributes(url, cont)
      val (figureTag, errors) = HtmlTagGenerator.buildEmbedContent(attributes ++ extraAttributes)
      (figureTag, requiredLibs, errors :+ message)
    }

    private def getExtraAttributes(url: String, cont: ContentBrowser): (Map[String, String], Option[RequiredLibrary]) = {
      val NRKUrlPattern = """(.*nrk.no)""".r
      url.host.getOrElse("") match {
        case NRKUrlPattern(_) => extraNrkAttributes(cont.get("nid"))
        case _ => (Map(), None)
      }
    }

    def extraNrkAttributes(nodeId: String): (Map[String, String], Option[RequiredLibrary]) = {
      val doc = Jsoup.parseBodyFragment(extractService.getNodeEmbedCode(nodeId).get)
      val (videoId, requiredLibrary) = (doc.select("div[data-nrk-id]").attr("data-nrk-id"), doc.select("script").attr("src"))
      (Map("nrk-video-id" -> videoId, "resource" -> "nrk"),
        Some(RequiredLibrary("text/javascript", "NRK video embed", requiredLibrary)))
    }

    private def insertDetailSummary(url: String, cont: ContentBrowser): (String, Option[RequiredLibrary], Seq[String]) = {
      val (embedFigure, requiredLib, figureErrors) = insertInline(url, cont)
      (s"<details><summary>${cont.get("link_text")}</summary>$embedFigure</details>", requiredLib, figureErrors)
    }

    private def insertAnchor(url: String, cont: ContentBrowser): (String, Option[RequiredLibrary], Seq[String]) = {
      val (htmlTag, errors) = HtmlTagGenerator.buildAnchor(url, cont.get("link_text"), Map("title" -> cont.get("link_title_text")))
      (s" $htmlTag", None, errors)
    }

    private def insertUnhandled(url: String, cont: ContentBrowser): (String, Option[RequiredLibrary], Seq[String]) = {
      val (anchor, requiredLib, anchorErrors) = insertAnchor(url, cont)
      val message = s"""Unhandled insertion method '${cont.get("insertion")}' on '${cont.get("link_text")}'. Defaulting to link."""

      logger.warn(message)
      (anchor, requiredLib, anchorErrors :+ message)
    }
  }

}
