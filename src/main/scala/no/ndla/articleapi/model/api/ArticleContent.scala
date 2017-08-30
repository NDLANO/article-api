/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}
import scala.annotation.meta.field

@ApiModel(description = "The content of the article in the specified language")
case class ArticleContent(@(ApiModelProperty@field)(description = "The html content") content: String,
                          @(ApiModelProperty@field)(description = "Foot notes referred to within the html article") footNotes: Option[Map[String, FootNoteItem]],
                          @(ApiModelProperty@field)(description = "ISO 639-1 code that represents the language used in title") language: String
                         )

case class ArticleContentV2(@(ApiModelProperty@field)(description = "The html content") content: String,
                            @(ApiModelProperty@field)(description = "Foot notes referred to within the html article") footNotes: Option[Map[String, FootNoteItem]]
                           )

