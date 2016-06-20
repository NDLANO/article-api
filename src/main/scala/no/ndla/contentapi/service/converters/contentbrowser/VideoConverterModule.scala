package no.ndla.contentapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.model.RequiredLibrary


trait VideoConverterModule {

  object VideoConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "video"

    override def convert(content: ContentBrowser): (String, List[RequiredLibrary], List[String]) = {
      val requiredLibrary = RequiredLibrary("text/javascript", "Brightcove video", "http://players.brightcove.net/4806596774001/BkLm8fT_default/index.min.js")
      val embedVideo = s"""<div style="display: block; position: relative; max-width: 100%;">
             <div style="padding-top: 56.25%;">
               <video
                 data-video-id="ref:${content.get("nid")}"
                 data-account="4806596774001"
                 data-player="BkLm8fT"
                 data-embed="default"
                 class="video-js"
                 controls
                 style="width: 100%; height: 100%; position: absolute; top: 0px; bottom: 0px; right: 0px; left: 0px;">
               </video>
             </div></div>"""

      val errorMsg = s"Added video with nid ${content.get("nid")}"
      logger.info(errorMsg)
      (embedVideo, List(requiredLibrary), List[String](errorMsg))
    }
  }
}
