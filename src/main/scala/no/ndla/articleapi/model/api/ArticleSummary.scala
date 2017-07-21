/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.api

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Short summary of information about the article")
case class ArticleSummary(@(ApiModelProperty@field)(description = "The unique id of the article") id: String,
                          @(ApiModelProperty@field)(description = "The title of the article") title: Seq[ArticleTitle],
                          @(ApiModelProperty@field)(description = "A visual element article") visualElement: Seq[VisualElement],
                          @(ApiModelProperty@field)(description = "An introduction for the article") introduction: Seq[ArticleIntroduction],
                          @(ApiModelProperty@field)(description = "The full url to where the complete information about the article can be found") url: String,
                          @(ApiModelProperty@field)(description = "Describes the license of the article") license: String,
                          @(ApiModelProperty@field)(description = "The type of article this is. Possible values are topic-article,standard") articleType: String
                         )

@ApiModel(description = "Short summary of information about the article")
case class ArticleSummaryV2(@(ApiModelProperty@field)(description = "The unique id of the article") id: Long,
                            @(ApiModelProperty@field)(description = "The title of the article") title: String,
                            @(ApiModelProperty@field)(description = "A visual element article") visualElement: String,
                            @(ApiModelProperty@field)(description = "An introduction for the article") introduction: String,
                            @(ApiModelProperty@field)(description = "The full url to where the complete information about the article can be found") url: String,
                            @(ApiModelProperty@field)(description = "Describes the license of the article") license: String,
                            @(ApiModelProperty@field)(description = "The type of article this is. Possible values are topic-article,standard") articleType: String,
                            @(ApiModelProperty @field)(description = "A list of available languages for this audio") supportedLanguages: Seq[String]
                           )

