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
import no.ndla.articleapi.service.converters.HtmlTagGenerator

trait ImageConverterModule {
  this: ImageApiClient with HtmlTagGenerator =>

  object ImageConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "image"

    override def convert(content: ContentBrowser, visitedNodes: Seq[String]): (String, Seq[RequiredLibrary], ImportStatus) = {
      val (replacement, errors) = getImage(content)
      logger.info(s"Converting image with nid ${content.get("nid")}")
      (replacement, List[RequiredLibrary](), ImportStatus(errors, visitedNodes))
    }

    def getImage(cont: ContentBrowser): (String, Seq[String]) = {
      val alignment = getImageAlignment(cont)
      imageApiClient.importOrGetMetaByExternId(cont.get("nid")) match {
        case Some(image) => HtmlTagGenerator.buildImageEmbedContent(
          caption=cont.get("link_text"),
          imageId=image.id,
          align=alignment.getOrElse(""),
          size=cont.get("imagecache").toLowerCase,
          altText=cont.get("alt"))
        case None =>
          (s"<img src='stock.jpeg' alt='The image with id ${cont.get("nid")} was not not found' />",
            Seq(s"Image with id ${cont.get("nid")} was not found"))
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
