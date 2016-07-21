package no.ndla.contentapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.service.{ExtractServiceComponent, StorageService}
import no.ndla.contentapi.ContentApiProperties.amazonUrlPrefix
import no.ndla.contentapi.model.{ImportStatus, RequiredLibrary}

trait AudioConverterModule  {
  this: ExtractServiceComponent with StorageService =>

  object AudioConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "audio"

    override def convert(content: ContentBrowser, visitedNodes: Seq[String]): (String, Seq[RequiredLibrary], ImportStatus) = {
      val requiredLibraries = List[RequiredLibrary]()
      val nodeId = content.get("nid")
      val audioMeta = extractService.getAudioMeta(nodeId)

      logger.info(s"Converting audio with nid $nodeId")

      audioMeta match {
        case Some(audio) => {
          val (filePath, uploadError) = storageService.uploadFileFromUrl(nodeId, audio) match {
            case Some(filepath) => (filepath, List())
            case None => {
              val msg = s"""Failed to upload audio (node $nodeId)"""
              logger.warn(msg)
              ("", List(msg))
            }
          }

          val player =
            s"""<figure>
              <figcaption>${audio.title}</figcaption>
              <audio src="$amazonUrlPrefix/$filePath" preload="auto" controls>
                Your browser does not support the <code>audio</code> element.
              </audio>
            </figure>
          """.stripMargin
          (player, requiredLibraries, ImportStatus(uploadError, visitedNodes))
        }
        case None => {
          val msg = s"""Failed to retrieve audio metadata for node $nodeId"""
          logger.warn(msg)
          (s"{Error: $msg}", requiredLibraries, ImportStatus(List(msg), visitedNodes))
        }
      }
    }

  }
}
