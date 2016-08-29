package no.ndla.articleapi.service.converters.contentbrowser
import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.model.{ImportStatus, RequiredLibrary}
import no.ndla.articleapi.service.{ExtractServiceComponent, StorageService}

trait FilConverterModule {
  this: ExtractServiceComponent with StorageService =>

  object FilConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "fil"

    override def convert(content: ContentBrowser, visitedNodes: Seq[String]): (String, Seq[RequiredLibrary], ImportStatus) = {
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
          (s"""<a href="$filePath">${fileMeta.fileName}</a>""", List[RequiredLibrary](), ImportStatus(uploadError, visitedNodes))
        }
        case None => {
          val message = s"File with node ID $nodeId was not found"
          logger.warn(message)
          ("", List[RequiredLibrary](), ImportStatus(List(message), visitedNodes))
        }
      }
    }

  }
}
