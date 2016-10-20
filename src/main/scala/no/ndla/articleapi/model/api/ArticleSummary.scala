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

@ApiModel(description = "Short summary of information about the article")
case class ArticleSummary(@(ApiModelProperty@field)(description = "The unique id of the article") id: String,
                          @(ApiModelProperty@field)(description = "The title of the article") title: Seq[ArticleTitle],
                          @(ApiModelProperty@field)(description = "The full url to where the complete information about the article can be found") url: String,
                          @(ApiModelProperty@field)(description = "Describes the license of the article") license: String)
