package no.ndla.contentapi.service.converters.contentbrowser
import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.model.RequiredLibrary
import no.ndla.contentapi.service.{ExtractServiceComponent, StorageService}

trait FilConverterModule {
  this: ExtractServiceComponent with StorageService =>

  object FilConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "fil"

    override def convert(content: ContentBrowser): (String, Seq[RequiredLibrary], Seq[String]) = {
      val nodeId = content.get("nid")

      extractService.getNodeFilMeta(nodeId) match {
        case Some(fileMeta) => {
          val (filePath, uploadError) = storageService.uploadFileFromUrl(nodeId, fileMeta) match {
            case Some(path) => (path, List())
            case None => {
              val msg = s"Failed to upload audio (node $nodeId)"
              logger.warn(msg)
              ("", List(msg))
            }
          }
          (s"""<a href="$filePath">${fileMeta.fileName}</a>""", List[RequiredLibrary](), uploadError)
        }
        case None => {
          val message = s"File with node ID $nodeId was not found"
          logger.warn(message)
          ("", List[RequiredLibrary](), List(message))
        }
      }
    }
  }
}
