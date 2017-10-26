/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.model.domain.{ImportStatus, RequiredLibrary}
import no.ndla.articleapi.integration.ImageApiClient
import no.ndla.articleapi.model.api.ImportException
import no.ndla.articleapi.service.converters.HtmlTagGenerator

import scala.util.{Failure, Success, Try}

trait ImageConverterModule {
  this: ImageApiClient with HtmlTagGenerator =>

  object ImageConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "image"

    override def convert(content: ContentBrowser, importStatus: ImportStatus): Try[(String, Seq[RequiredLibrary], ImportStatus)] = {
      val nodeId = content.get("nid")
      logger.info(s"Converting image with nid $nodeId")
      val a = getImage(content).map(imageHtml => (imageHtml, Seq(), importStatus)) match {
        case Success(x) => Success(x)
        case Failure(_) => Failure(ImportException(s"Failed to import image with node id $nodeId"))
      }
      a
    }

    def getImage(cont: ContentBrowser): Try[String] = {
      val alignment = getImageAlignment(cont)
      toImageEmbed(cont.get("nid"), cont.get("link_text"), alignment.getOrElse(""), cont.get("imagecache").toLowerCase, cont.get("alt"))
    }

    def toImageEmbed(nodeId: String, caption: String, align: String, size: String, altText: String): Try[String] = {
      imageApiClient.importImage(nodeId) match {
        case Some(image) =>
          Success(HtmlTagGenerator.buildImageEmbedContent(caption, image.id, align, size, altText))
        case None =>
          Failure(ImportException(s"Failed to import image with ID $nodeId"))
      }
    }

    private def getImageAlignment(cont: ContentBrowser): Option[String] = {
      val marginCssClass = cont.get("css_class").split(" ").find(_.contains("margin"))
      val margin = marginCssClass.flatMap(margin => """contentbrowser_margin_(left|right)$""".r.findFirstMatchIn(margin).map(_.group(1)))

      // right margin = left alignment, left margin = right alignment
      margin match {
        case Some("right") => Some("left")
        case Some("left") => Some("right")
        case _ => None
      }
    }

  }
}
