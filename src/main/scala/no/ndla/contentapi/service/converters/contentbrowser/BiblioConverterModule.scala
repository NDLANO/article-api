package no.ndla.contentapi.service.converters.contentbrowser
import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.model.{ImportStatus, RequiredLibrary}
import no.ndla.contentapi.repository.ContentRepositoryComponent
import no.ndla.contentapi.service.{ExtractConvertStoreContent, ExtractServiceComponent}

trait BiblioConverterModule {
  this: ExtractServiceComponent with ExtractConvertStoreContent with ContentRepositoryComponent =>

  object BiblioConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "biblio"

    override def convert(content: ContentBrowser, visitedNodes: Seq[String]): (String, Seq[RequiredLibrary], ImportStatus) = {
      val nodeId = content.get("nid")
      (s"""<a id="biblio-$nodeId"></a>""", List[RequiredLibrary](), ImportStatus(Seq(), visitedNodes))
    }
  }
}
