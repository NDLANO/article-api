/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Information about the article")
case class NewArticle(@(ApiModelProperty@field)(description = "The title of the article") title: Seq[ArticleTitle],
                      @(ApiModelProperty@field)(description = "The content of the article") content: Seq[ArticleContent],
                      @(ApiModelProperty@field)(description = "Searchable tags for the article") tags: Seq[ArticleTag],
                      @(ApiModelProperty@field)(description = "An introduction for the article") introduction: Option[Seq[ArticleIntroduction]],
                      @(ApiModelProperty@field)(description = "Describes the copyright information for the article") copyright: Copyright,
                      @(ApiModelProperty@field)(description = "Required libraries in order to render the article") requiredLibraries: Option[Seq[RequiredLibrary]],
                      @(ApiModelProperty@field)(description = "The type of learning resource") contentType: String)
