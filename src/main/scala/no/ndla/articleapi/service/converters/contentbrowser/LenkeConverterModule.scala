/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import no.ndla.articleapi.model.{ImportStatus, RequiredLibrary}
import com.netaporter.uri.dsl._
import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.service.ExtractServiceComponent
import no.ndla.articleapi.service.converters.HtmlFigureGenerator

trait LenkeConverterModule {
  this: ExtractServiceComponent =>

  object LenkeConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "lenke"

    override def convert(content: ContentBrowser, visitedNodes: Seq[String]): (String, Seq[RequiredLibrary], ImportStatus) = {
      val (replacement, errors) = convertLink(content)
      logger.info(s"Converting lenke with nid ${content.get("nid")}")
      (replacement, List[RequiredLibrary](), ImportStatus(errors, visitedNodes))
    }

    def convertLink(cont: ContentBrowser): (String, Seq[String]) = {
      var errors = Seq[String]()
      val url = extractService.getNodeEmbedData(cont.get("nid")).get
      val NDLAPattern = """.*(ndla.no).*""".r

      url.host match {
        case NDLAPattern(_) => {
          errors = errors :+ s"(Warning) Link to NDLA resource '$url'"
          logger.warn("Link to NDLA resource: '{}'", url)
        }
        case _ =>
      }


      val (embedMeta, figureUsageErrors) = HtmlFigureGenerator.buildFigure(Map(
        "resource" -> "external", "id" -> s"${cont.id}", "url" -> url))
      errors = errors ++ figureUsageErrors

      val insertionMethod = cont.get("insertion")
      val converted = insertionMethod match {
        case "inline" => embedMeta
        case "link" | "lightbox_large" => s"""<a href="$url" title="${cont.get("link_title_text")}">${cont.get("link_text")}</a>"""
        case "collapsed_body" => {
          s"<details><summary>${cont.get("link_text")}</summary>$embedMeta</details>"
        }
        case _ => {
          val message = s"""Unhandled fagstoff insertion method '$insertionMethod' on '${cont.get("link_text")}'. Defaulting to link."""
          logger.warn(message)
          errors = errors :+ message
          s"""<a href="$url" title="${cont.get("link_title_text")}">${cont.get("link_text")}</a>"""
        }
      }
      (converted, errors)
    }
  }
}
