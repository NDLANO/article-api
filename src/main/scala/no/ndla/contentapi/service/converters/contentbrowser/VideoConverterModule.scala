package no.ndla.contentapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.model.RequiredLibrary
import no.ndla.contentapi.ContentApiProperties.{NDLABrightcoveAccountId, NDLABrightcovePlayerId}

trait VideoConverterModule {

  object VideoConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "video"

    override def convert(content: ContentBrowser): (String, List[RequiredLibrary], List[String]) = {
      val requiredLibrary = RequiredLibrary("text/javascript", "Brightcove video", s"http://players.brightcove.net/$NDLABrightcoveAccountId/${NDLABrightcovePlayerId}_default/index.min.js")
      val embedVideoMeta = s"""<figure data-resource="brightcove" data-id="ref:${content.get("nid")}" data-account="$NDLABrightcoveAccountId" data-player="$NDLABrightcovePlayerId"></figure>"""

      val errorMsg = s"Added video with nid ${content.get("nid")}"
      logger.info(errorMsg)
      (embedVideoMeta, List(requiredLibrary), List[String](errorMsg))
    }
  }
}
