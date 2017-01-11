/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.converters

import no.ndla.articleapi.ArticleApiProperties._
import no.ndla.articleapi.service.SequenceGenerator
import no.ndla.articleapi.service.converters.HTMLCleaner.isAttributeKeyValid

trait HtmlTagGenerator {
  this: SequenceGenerator =>

  object HtmlTagGenerator {
    def buildEmbedContent(dataAttributes: Map[String, String]): (String, Seq[String]) = {
      val attributesWithPrefix = prefixKeyWith(dataAttributes + ("id" -> nextNumberInSequence), "data-")
      val errorMessages = verifyAttributeKeys(attributesWithPrefix.keySet, resourceHtmlEmbedTag)

      (s"<$resourceHtmlEmbedTag ${buildAttributesString(attributesWithPrefix)} />", errorMessages)
    }

    def buildErrorContent(message: String): (String, Seq[String]) =
      buildEmbedContent(Map("resource" -> ResourceType.Error, "message" -> message))

    def buildImageEmbedContent(caption: String, imageId: String, align: String, size: String, altText: String) = {
      val dataAttributes = Map(
        "resource" -> ResourceType.Image,
        "resource_id" -> imageId,
        "size" -> size,
        "alt" -> altText,
        "caption" -> caption,
        "align" -> align)

      buildEmbedContent(dataAttributes)
    }

    def buildAudioEmbedContent(audioId: String) = {
      val dataAttributes = Map("resource" -> ResourceType.Audio, "resource_id" -> audioId)
      buildEmbedContent(dataAttributes)
    }

    def buildH5PEmbedContent(url: String) = {
      val dataAttributes = Map("resource" -> ResourceType.H5P, "url" -> url)
      buildEmbedContent(dataAttributes)
    }

    def buildBrightCoveEmbedContent(caption: String, videoId: String, account: String, player: String) = {
      val dataAttributes = Map(
        "resource" -> ResourceType.Brightcove,
        "caption" -> caption,
        "videoid" -> videoId,
        "account" -> account,
        "player" -> player
      )
      buildEmbedContent(dataAttributes)
    }

    def buildLinkEmbedContent(contentId: String, linkText: String) = {
      val dataAttributes = Map(
        "resource" -> ResourceType.ContentLink,
        "content-id" -> contentId,
        "link-text" -> linkText)
      buildEmbedContent(dataAttributes)
    }

    def buildExternalInlineEmbedContent(url: String) = {
      val dataAttributes = Map(
        "resource" -> ResourceType.ExternalContent,
        "url" -> url
      )
      buildEmbedContent(dataAttributes)
    }

    def buildNRKInlineVideoContent(nrkVideoId: String, url: String) = {
      val dataAttributes = Map(
        "resource" -> ResourceType.NRKContent,
        "nrk-video-id" -> nrkVideoId,
        "url" -> url
      )
      buildEmbedContent(dataAttributes)
    }

    def buildAnchor(href: String, anchorText: String, extraAttributes: Map[String, String] = Map()): (String, Seq[String]) = {
      val attributes = extraAttributes + ("href" -> href)
      val errorMessages = verifyAttributeKeys(attributes.keySet, "a")
      (s"<a ${buildAttributesString(attributes)}>$anchorText</a>", errorMessages)
    }

    private def prefixKeyWith(attributeMap: Map[String, String], prefix: String): Map[String, String] =
      attributeMap.map { case (key, value) => s"$prefix$key" -> value }

    private def buildAttributesString(figureDataAttributeMap: Map[String, String]): String =
      figureDataAttributeMap.toList.sortBy(_._1).map { case (key, value) => s"""$key="${value.trim}"""" }.mkString(" ")

    private def verifyAttributeKeys(attributeKeys: Set[String], tagName: String): Seq[String] =
      attributeKeys.flatMap(attributeKey => {
        isAttributeKeyValid(attributeKey, tagName) match {
          case true => None
          case false => Some(s"This is a BUG: Trying to use illegal attribute $attributeKey!")
        }
      }).toSeq

  }

}

object ResourceType {
  val Error = "error"
  val Image = "image"
  val Audio = "audio"
  val H5P = "h5p"
  val Brightcove = "brightcove"
  val ContentLink = "content-link"
  val ExternalContent = "external"
  val NRKContent = "nrk"
}
