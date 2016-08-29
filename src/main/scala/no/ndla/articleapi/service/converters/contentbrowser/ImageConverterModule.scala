package no.ndla.articleapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.model.{ImportStatus, RequiredLibrary}
import no.ndla.articleapi.service.ImageApiServiceComponent
import no.ndla.articleapi.ContentApiProperties.imageApiUrl

trait ImageConverterModule {
  this: ImageApiServiceComponent =>

  object ImageConverter extends ContentBrowserConverterModule with LazyLogging{
    override val typeName: String = "image"

    override def convert(content: ContentBrowser, visitedNodes: Seq[String]): (String, Seq[RequiredLibrary], ImportStatus) = {
      val (replacement, errors) = getImage(content)
      logger.info(s"Converting image with nid ${content.get("nid")}")
      (replacement, List[RequiredLibrary](), ImportStatus(errors, visitedNodes))
    }

    def getImage(cont: ContentBrowser): (String, Seq[String]) = {
      var errors = Seq[String]()
      val imageSizeHint = cont.get("imagecache").toLowerCase

      val imageTag = imageApiService.getMetaByExternId(cont.get("nid")) match {
        case Some(image) =>
          s"""<figure data-resource="image" data-id="${cont.id}" data-url="$imageApiUrl/${image.id}" data-size="$imageSizeHint"></figure>"""
        case None => {
            imageApiService.importImage(cont.get("nid")) match {
            case Some(image) =>
              s"""<figure data-resource="image" data-id="${cont.id}" data-url="$imageApiUrl/${image.id}" data-size="$imageSizeHint"></figure>"""
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
