package no.ndla.contentapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.integration.ConverterModule
import no.ndla.contentapi.model.RequiredLibrary
import no.ndla.contentapi.service.ExtractServiceComponent
import org.jsoup.nodes.Element

trait ContentBrowserConverter {
  this: ExtractServiceComponent with ImageConverterModule with LenkeConverterModule with H5PConverterModule =>
  val contentBrowserConverter: ContentBrowserConverter

  class ContentBrowserConverter extends ConverterModule with LazyLogging {
    private val contentBrowserModules = Map[String, ContentBrowserConverterModule](
      ImageConverter.typeName -> ImageConverter,
      H5PConverter.typeName -> H5PConverter,
      LenkeConverter.typeName -> LenkeConverter)

    def convert(el: Element): (Element, List[RequiredLibrary], List[String]) = {
      var isContentBrowserField = false
      var requiredLibraries = List[RequiredLibrary]()
      var errors = List[String]()

      do {
        val text = el.html()
        val cont = ContentBrowser(text)

        isContentBrowserField = cont.isContentBrowserField()
        if (isContentBrowserField) {
          val nodeType = extractService.getNodeType(cont.get("nid")).getOrElse("UNKNOWN")

          val (newContent, reqLibs, errorMsgs) = contentBrowserModules.get(nodeType) match {
            case Some(module) => module.convert(cont)
            case None => {
              val nodeId = cont.get("nid")
              logger.warn(s"{Unsupported content '${nodeType}': ${nodeId}}")
              val replacement = s"{Unsupported content '${nodeType}': ${nodeId}}"
              (replacement, List[RequiredLibrary](), List(s"{Unsupported content '${nodeType}': ${nodeId}}"))
            }
          }
          requiredLibraries = requiredLibraries ::: reqLibs
          errors = errors ::: errorMsgs

          val (start, end) = cont.getStartEndIndex()
          el.html(text.substring(0, start) + newContent + text.substring(end))
        }
      } while (isContentBrowserField)

      (el, requiredLibraries, errors)
    }
  }
}
