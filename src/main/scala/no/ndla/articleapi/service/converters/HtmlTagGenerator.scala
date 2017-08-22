/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.converters

import no.ndla.articleapi.ArticleApiProperties._

trait HtmlTagGenerator {

  object HtmlTagGenerator {
    def buildEmbedContent(dataAttributes: Map[Attributes.Value, String]): String = {
      s"<$resourceHtmlEmbedTag ${buildAttributesString(dataAttributes)} />"
    }

    def buildErrorContent(message: String): String =
      buildEmbedContent(Map(Attributes.DataResource -> ResourceType.Error.toString, Attributes.DataMessage -> message))

    def buildImageEmbedContent(caption: String, imageId: String, align: String, size: String, altText: String) = {
      val dataAttributes = Map(
        Attributes.DataResource -> ResourceType.Image.toString,
        Attributes.DataResource_Id -> imageId,
        Attributes.DataSize -> size,
        Attributes.DataAlt -> altText,
        Attributes.DataCaption -> caption,
        Attributes.DataAlign -> align)

      buildEmbedContent(dataAttributes)
    }

    def buildAudioEmbedContent(audioId: String) = {
      val dataAttributes = Map(Attributes.DataResource -> ResourceType.Audio.toString, Attributes.DataResource_Id -> audioId)
      buildEmbedContent(dataAttributes)
    }

    def buildH5PEmbedContent(url: String) = {
      val dataAttributes = Map(Attributes.DataResource -> ResourceType.H5P.toString, Attributes.DataUrl -> url)
      buildEmbedContent(dataAttributes)
    }

    def buildBrightCoveEmbedContent(caption: String, videoId: String, account: String, player: String) = {
      val dataAttributes = Map(
        Attributes.DataResource -> ResourceType.Brightcove.toString,
        Attributes.DataCaption -> caption,
        Attributes.DataVideoId -> videoId,
        Attributes.DataAccount -> account,
        Attributes.DataPlayer -> player
      )
      buildEmbedContent(dataAttributes)
    }

    def buildLinkEmbedContent(contentId: String, linkText: String) = {
      val dataAttributes = Map(
        Attributes.DataResource -> ResourceType.ContentLink.toString,
        Attributes.DataContentId -> contentId,
        Attributes.DataLinkText -> linkText)
      buildEmbedContent(dataAttributes)
    }

    def buildConceptEmbedContent(conceptId: Long, linkText: String) = {
      val dataAttributes = Map(
        Attributes.DataResource -> ResourceType.ConceptLink.toString,
        Attributes.DataContentId -> s"$conceptId",
        Attributes.DataLinkText -> linkText)
      buildEmbedContent(dataAttributes)
    }

    def buildExternalInlineEmbedContent(url: String) = {
      val dataAttributes = Map(
        Attributes.DataResource -> ResourceType.ExternalContent.toString,
        Attributes.DataUrl -> url
      )
      buildEmbedContent(dataAttributes)
    }

    def buildNRKInlineVideoContent(nrkVideoId: String, url: String) = {
      val dataAttributes = Map(
        Attributes.DataResource -> ResourceType.NRKContent.toString,
        Attributes.DataNRKVideoId -> nrkVideoId,
        Attributes.DataUrl -> url
      )
      buildEmbedContent(dataAttributes)
    }

    def buildDetailsSummaryContent(linkText: String, content: String) = {
      s"<details><summary>$linkText</summary>$content</details>"
    }

    def buildAnchor(href: String, anchorText: String, title: String): String = {
      val attributes = Map(Attributes.Href -> href, Attributes.Title -> title)
      s"<a ${buildAttributesString(attributes)}>$anchorText</a>"
    }

    private def buildAttributesString(figureDataAttributeMap: Map[Attributes.Value, String]): String =
      figureDataAttributeMap.toList.sortBy(_._1.toString).map { case (key, value) => s"""$key="${value.trim}"""" }.mkString(" ")

  }

}

object ResourceType extends Enumeration {
  val Error = Value("error")
  val Image = Value("image")
  val Audio = Value("audio")
  val H5P = Value("h5p")
  val Brightcove = Value("brightcove")
  val ContentLink = Value("content-link")
  val ExternalContent = Value("external")
  val NRKContent = Value("nrk")
  val ConceptLink = Value("concept")

  def all: Set[String] = ResourceType.values.map(_.toString)

  def valueOf(s: String): Option[ResourceType.Value] = {
    ResourceType.values.find(_.toString == s)
  }
}

object Attributes extends Enumeration {
  val DataId = Value("data-id")
  val DataUrl = Value("data-url")
  val DataAlt = Value("data-alt")
  val DataSize = Value("data-size")
  val DataAlign = Value("data-align")
  val DataPlayer = Value("data-player")
  val DataMessage = Value("data-message")
  val DataCaption = Value("data-caption")
  val DataAccount = Value("data-account")
  val DataVideoId = Value("data-videoid")
  val DataResource = Value("data-resource")
  val DataLinkText = Value("data-link-text")
  val DataContentId = Value("data-content-id")
  val DataNRKVideoId = Value("data-nrk-video-id")
  val DataResource_Id = Value("data-resource_id")

  val XMLNsAttribute = Value("xmlns")

  val Href = Value("href")
  val Title = Value("title")
  val Align = Value("align")
  val Valign = Value("valign")

  val DataType = Value("data-type")

  def all: Set[String] = Attributes.values.map(_.toString)

  def valueOf(s: String): Option[Attributes.Value] = {
    Attributes.values.find(_.toString == s)
  }
}

