/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.integration

import no.ndla.articleapi.ArticleApiProperties
import no.ndla.articleapi.model.domain.{Author, Copyright}
import no.ndla.network.NdlaClient

import scala.util.{Success, Try}
import scalaj.http.{Http, HttpRequest}

trait ImageApiClient {
  this: NdlaClient =>
  val imageApiClient: ImageApiClient

  class ImageApiClient {
    private val imageApiInternEndpointURL = s"http://${ArticleApiProperties.ImageHost}/intern"
    private val imageApiImportImageURL = s"$imageApiInternEndpointURL/import/:external_id"
    private val imageApiGetByExternalIdURL = s"$imageApiInternEndpointURL/extern/:external_id"
    private val ImageApiHealthEndpoint = s"http://${ArticleApiProperties.ImageHost}/health"

    def getMetaByExternId(externId: String): Option[ImageMetaInformation] = {
      val request: HttpRequest = Http(s"$imageApiGetByExternalIdURL".replace(":external_id", externId))
      ndlaClient.fetchWithForwardedAuth[ImageMetaInformation](request).toOption
    }

    def importImage(externId: String): Option[ImageMetaInformation] = {
      val second = 1000
      val request: HttpRequest = Http(s"$imageApiImportImageURL".replace(":external_id", externId)).timeout(20 * second, 20 * second).postForm
      ndlaClient.fetchWithForwardedAuth[ImageMetaInformation](request).toOption
    }

    def importOrGetMetaByExternId(externId: String): Option[ImageMetaInformation] = {
      getMetaByExternId(externId) match {
        case None => importImage(externId)
        case Some(image) => Some(image)
      }
    }

    def isHealthy: Boolean = {
      Try(Http(ImageApiHealthEndpoint).timeout(1000,15000).execute()) match {
        case Success(resp) => resp.isSuccess
        case _ => false
      }
    }

  }
}

case class ImageMetaInformation(id:String, titles:List[ImageTitle], alttexts:List[ImageAltText], imageUrl:String, size:Int, contentType:String, copyright: ImageCopyright, tags: ImageTag)
case class ImageCopyright(license: ImageLicense, origin: String, authors: Seq[Author])
case class ImageLicense(license: String, description: String, url: Option[String])
case class ImageTitle(title:String, language:Option[String])
case class ImageAltText(alttext:String, language:Option[String])
case class ImageTag(tags: Seq[String], language:Option[String])

