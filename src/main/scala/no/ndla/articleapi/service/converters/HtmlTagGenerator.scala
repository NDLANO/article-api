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
      buildEmbedContent(Map("resource" -> "error", "message" -> message))

    def buildImageEmbedContent(caption: String, imageId: String, align: String, size: String, altText: String) = {
      val dataAttributes = Map(
        "resource" -> "image",
        "resourceId" -> imageId,
        "size" -> size,
        "alt" -> altText,
        "caption" -> caption,
        "align" -> align)

      buildEmbedContent(dataAttributes)
    }

    def buildAudioEmbedContent(audioId: String) = {
      val dataAttributes = Map(
        "resource" -> "audio",
        "resourceId" -> audioId
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