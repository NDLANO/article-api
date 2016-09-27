/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.converters

import no.ndla.articleapi.ArticleApiProperties

object HtmlFigureGenerator {
  def buildFigure(dataAttributes: Map[String, String]): (String, Seq[String]) = {
    val errorMessages = verifyAttributeKeys(dataAttributes.keySet)
    (s"<figure ${buildDataAttributesString(dataAttributes)}></figure>", errorMessages.toSeq)
  }

  private def buildDataAttributesString(figureDataAttributeMap: Map[String, String]): String =
    figureDataAttributeMap.toList.map { case (key, value) => s"""data-$key="${value.trim}""""}.mkString(" ")

  private def verifyAttributeKeys(attributeKeys: Set[String]): Set[String] =
    attributeKeys.flatMap(key => {
      val dataKey = s"data-$key"
      isAttributeKeyValid(dataKey) match {
        case true => None
        case false => Some(s"This is a BUG: Trying to use illegal attribute $dataKey!")
      }
    })

  private def isAttributeKeyValid(attributeKey: String): Boolean =
    ArticleApiProperties.permittedHTMLAttributes.contains(attributeKey)

}
