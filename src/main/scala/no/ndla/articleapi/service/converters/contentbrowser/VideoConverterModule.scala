/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.model.{ImportStatus, RequiredLibrary}
import no.ndla.articleapi.ArticleApiProperties.{NDLABrightcoveAccountId, NDLABrightcovePlayerId}
import no.ndla.articleapi.service.converters.HtmlFigureGenerator

trait VideoConverterModule {

  object VideoConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "video"

    override def convert(content: ContentBrowser, visitedNodes: Seq[String]): (String, Seq[RequiredLibrary], ImportStatus) = {
      val requiredLibrary = RequiredLibrary("text/javascript", "Brightcove video", s"http://players.brightcove.net/$NDLABrightcoveAccountId/${NDLABrightcovePlayerId}_default/index.min.js")
      val (embedVideoMeta, errors) = HtmlFigureGenerator.buildFigure(Map(
        "resource" -> "brightcove",
        "id" -> s"${content.id.toString}",
        "videoid" -> s"ref:${content.get("nid")}",
        "account" -> s"$NDLABrightcoveAccountId",
        "player" -> s"$NDLABrightcovePlayerId"
      ))

      logger.info(s"Added video with nid ${content.get("nid")}")
      (embedVideoMeta, List(requiredLibrary), ImportStatus(errors, visitedNodes))
    }
  }
}
