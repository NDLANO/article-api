package no.ndla.contentapi.service

import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.model.Copyright
import org.json4s.DefaultFormats
import org.json4s.native.Serialization._
import no.ndla.contentapi.ContentApiProperties.{imageApiBaseUrl, imageApiImportImageURL, imageApiGetByExternalIdURL}
import scalaj.http.{Http, HttpRequest}

trait ImageApiServiceComponent {
  val imageApiService: ImageApiService

  class ImageApiService extends LazyLogging {
    implicit val formats = DefaultFormats

    def getMetaByExternId(externId: String): Option[ImageMetaInformation] = {
      val request: HttpRequest = Http(s"""$imageApiBaseUrl/$imageApiGetByExternalIdURL/$externId""")
      val response = request.asString
      response.isError match {
        case false => {
          try {
            val img = read[ImageMetaInformation](response.body)
            Option(img)
          } catch {
            case e: Exception => {
              logger.error(s"Couldn't get image for request = ${request.url}. Error was ${e.getMessage}")
              None
            }
          }
        }
        case true => None
      }
    }

    def importImage(externId: String): Option[ImageMetaInformation] = {
      val second = 1000
      val request: HttpRequest = Http(s"""$imageApiBaseUrl/$imageApiImportImageURL/${externId}""").timeout(15 * second, 15 * second).postForm
      val response = request.asString

      response.isError match {
        case true => {
          logger.warn(s"Failed to retrieve image with id {} from '{}': ${response.code}", externId, request.url)
          None
        }
        case false => getMetaByExternId(externId)
      }
    }

    def importOrGetMetaByExternId(externId: String): Option[ImageMetaInformation] = {
      getMetaByExternId(externId) match {
        case Some(image) => Some(image)
        case None => importImage(externId)
      }
    }
  }
}

case class ImageMetaInformation(id:String, titles:List[ImageTitle], alttexts:List[ImageAltText], images:ImageVariants, copyright:Copyright, tags:List[ImageTag])
case class ImageTitle(title:String, language:Option[String])
case class ImageAltText(alttext:String, language:Option[String])
case class ImageTag(tags: Seq[String], language:Option[String])
case class ImageVariants(small: Option[Image], full: Option[Image])
case class Image(url:String, size:Int, contentType:String)
