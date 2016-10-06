/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.model.{ImportStatus, RequiredLibrary}
import no.ndla.articleapi.service.ImageApiServiceComponent
import no.ndla.articleapi.ArticleApiProperties.imageApiUrl
import no.ndla.articleapi.service.converters.HtmlFigureGenerator

trait ImageConverterModule {
  this: ImageApiServiceComponent =>

  object ImageConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "image"

    override def convert(content: ContentBrowser, visitedNodes: Seq[String]): (String, Seq[RequiredLibrary], ImportStatus) = {
      val (replacement, errors) = getImage(content)
      logger.info(s"Converting image with nid ${content.get("nid")}")
      (replacement, List[RequiredLibrary](), ImportStatus(errors, visitedNodes))
    }

    def getImage(cont: ContentBrowser): (String, Seq[String]) = {
      val alignment = getImageAlignment(cont)
      val figureDataAttributes = Map(
        "resource" -> "image",
        "size" -> cont.get("imagecache").toLowerCase,
        "alt" -> cont.get("alt"),
        "caption" -> cont.get("link_text"),
        "id" -> s"${cont.id}",
        "align" -> alignment.getOrElse("")
      )

      imageApiService.importOrGetMetaByExternId(cont.get("nid")) match {
        case Some(image) =>
          HtmlFigureGenerator.buildFigure(figureDataAttributes + ("url" -> s"$imageApiUrl/${image.id}"))
        case None =>
          (s"<img src='stock.jpeg' alt='The image with id ${cont.get("nid")} was not not found' />",
            Seq(s"Image with id ${cont.get("nid")} was not found"))
      }
    }

    private def getImageAlignment(cont: ContentBrowser): Option[String] = {
      val marginCssClass = cont.get("css_class").split(" ").find(_.contains("margin"))
      val margin = marginCssClass.flatMap(margin =>"""contentbrowser_margin_(left|right)$""".r.findFirstMatchIn(margin).map(_.group(1)))

      // right margin = left alignment, left margin = right alignment
      margin match {
        case Some("right") => Some("left")
        case Some("left") => Some("right")
        case _ => None
      }
    }

  }
}
