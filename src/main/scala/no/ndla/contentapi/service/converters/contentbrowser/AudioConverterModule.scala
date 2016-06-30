package no.ndla.contentapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.service.{ExtractServiceComponent, StorageService}
import no.ndla.contentapi.ContentApiProperties.amazonUrlPrefix
import no.ndla.contentapi.model.RequiredLibrary

trait AudioConverterModule  {
  this: ExtractServiceComponent with StorageService =>

  object AudioConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "audio"

    override def convert(content: ContentBrowser): (String, List[RequiredLibrary], List[String]) = {
      val errors = List[String]()
      val requiredLibraries = List[RequiredLibrary]()
      val nodeId = content.get("nid")
      val audioMeta = extractService.getAudioMeta(nodeId)

      logger.info(s"Converting audio with nid $nodeId")

      audioMeta match {
        case Some(audio) => {
          val filepath = storageService.uploadAudiofromUrl(nodeId, audio)
          val player =
            s"""<figure>
                  <figcaption>${audio.title}</figcaption>
                  <audio src="$amazonUrlPrefix/$filepath" preload="auto" controls>
                    Your browser does not support the <code>audio</code> element.
                  </audio>
                </figure>
            """.stripMargin
          (player, requiredLibraries, errors)
        }
        case None => {
          val msg = s"""Failed to retrieve audio metadata for node $nodeId"""
          logger.warn(msg)
          (s"{Error: $msg}", requiredLibraries, errors :+ msg)
        }
      }
    }
  }
}
