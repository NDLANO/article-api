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
          val filePath = storageService.uploadFileFromUrl(nodeId, fileMeta)
          (s"""<a href="$filePath">${fileMeta.fileName}</a>""", List[RequiredLibrary](), List[String]())
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
