package no.ndla.contentapi.service.converters.contentbrowser

import no.ndla.contentapi.model.RequiredLibrary
import no.ndla.contentapi.service.ImageApiServiceComponent

trait ImageConverterModule {
  this: ImageApiServiceComponent =>

  object ImageConverter extends ContentBrowserConverterModule {
    override val typeName: String = "image"

    override def convert(content: ContentBrowser): (String, List[RequiredLibrary], List[String]) = {
      val (replacement, errors) = getImage(content)
      (replacement, List[RequiredLibrary](), errors)
    }

    def getImage(cont: ContentBrowser): (String, List[String]) = {
      var errors = List[String]()

      val imageTag = imageApiService.getMetaByExternId(cont.get("nid")) match {
        case Some(image) => s"""<img src="/images/${image.images.full.get.url}" alt="${cont.get("alt")}" />"""
        case None => {
            imageApiService.importImage(cont.get("nid")) match {
            case Some(image) => s"""<img src="/images/${image.images.full.get.url}" alt="${cont.get("alt")}" />"""
            case None => {
              errors = errors :+ s"Image with id ${cont.get("nid")} was not found"
              s"<img src='stock.jpeg' alt='The image with id ${cont.get("nid")} was not not found' />"
            }
          }
        }
      }
      (imageTag, errors)
    }
  }
}
