package no.ndla.contentapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.model.RequiredLibrary
import no.ndla.contentapi.service.{ImageApiServiceComponent, ImageMetaInformation}

trait ImageConverterModule {
  this: ImageApiServiceComponent =>

  object ImageConverter extends ContentBrowserConverterModule with LazyLogging{
    override val typeName: String = "image"

    override def convert(content: ContentBrowser): (String, Seq[RequiredLibrary], Seq[String]) = {
      val (replacement, errors) = getImage(content)
      logger.info(s"Converting image with nid ${content.get("nid")}")
      (replacement, List[RequiredLibrary](), errors)
    }

    def getImage(cont: ContentBrowser): (String, Seq[String]) = {
      var errors = Seq[String]()
      val imageSizeHint = cont.get("imagecache").toLowerCase

      val imageTag = imageApiService.getMetaByExternId(cont.get("nid")) match {
        case Some(image) => s"""<img class="$imageSizeHint" src="/images/${image.images.full.get.url}" alt="${cont.get("alt")}" />"""
        case None => {
            imageApiService.importImage(cont.get("nid")) match {
            case Some(image) => s"""<img class="$imageSizeHint" src="/images/${image.images.full.get.url}" alt="${cont.get("alt")}" />"""
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
