package no.ndla.contentapi.service.converters.contentbrowser
import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.model.RequiredLibrary
import no.ndla.contentapi.service.ExtractServiceComponent

trait BiblioConverterModule {
  this: ExtractServiceComponent =>

  object BiblioConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "biblio"

    override def convert(content: ContentBrowser): (String, Seq[RequiredLibrary], Seq[String]) = {
      val nodeId = content.get("nid")
      (s"""<a id="biblio-$nodeId"></a>""", List[RequiredLibrary](), List[String]())
    }
  }
}
