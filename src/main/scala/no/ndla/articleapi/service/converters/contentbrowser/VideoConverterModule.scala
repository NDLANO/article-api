/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.ArticleApiProperties.{NDLABrightcoveVideoScriptUrl, NDLABrightcoveAccountId, NDLABrightcovePlayerId}
import no.ndla.articleapi.model.domain.{ImportStatus, RequiredLibrary}
import no.ndla.articleapi.service.converters.HtmlTagGenerator

trait VideoConverterModule {
  this: HtmlTagGenerator =>

  object VideoConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "video"

    override def convert(content: ContentBrowser, visitedNodes: Seq[String]): (String, Seq[RequiredLibrary], ImportStatus) = {
      val requiredLibrary = RequiredLibrary("text/javascript", "Brightcove video", NDLABrightcoveVideoScriptUrl)
      val (embedVideoMeta, errors) = HtmlTagGenerator.buildBrightCoveEmbedContent(
        caption=content.get("link_text"),
        videoId=s"ref:${content.get("nid")}",
        account=s"$NDLABrightcoveAccountId",
        player=s"$NDLABrightcovePlayerId")

      logger.info(s"Added video with nid ${content.get("nid")}")
      (embedVideoMeta, List(requiredLibrary), ImportStatus(errors, visitedNodes))
    }
  }
}
