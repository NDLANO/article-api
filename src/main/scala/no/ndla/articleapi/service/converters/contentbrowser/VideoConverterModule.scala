/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.ArticleApiProperties.{NDLABrightcoveAccountId, NDLABrightcovePlayerId, NDLABrightcoveVideoScriptUrl}
import no.ndla.articleapi.model.domain.{ImportStatus, RequiredLibrary}
import no.ndla.articleapi.service.converters.HtmlTagGenerator
import scala.util.{Success, Try}

trait VideoConverterModule {
  this: HtmlTagGenerator =>

  object VideoConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "video"

    override def convert(content: ContentBrowser, visitedNodes: Seq[String]): Try[(String, Seq[RequiredLibrary], ImportStatus)] = {
      val (embedVideo, requiredLibrary) = toVideo(content.get("link_text"), content.get("nid"))
      logger.info(s"Added video with nid ${content.get("nid")}")
      Success(embedVideo, List(requiredLibrary), ImportStatus(Seq(), visitedNodes))
    }

    def toVideo(linkText: String, nodeId: String): (String, RequiredLibrary) = {
      val requiredLibrary = RequiredLibrary("text/javascript", "Brightcove video", NDLABrightcoveVideoScriptUrl)
      val embedVideoMeta = HtmlTagGenerator.buildBrightCoveEmbedContent(
        caption=linkText,
        videoId=s"ref:$nodeId",
        account=s"$NDLABrightcoveAccountId",
        player=s"$NDLABrightcovePlayerId")

      (embedVideoMeta, requiredLibrary)
    }

  }
}
