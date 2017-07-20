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

@ApiModel(description = "Information about the concept")
case class Concept(@(ApiModelProperty@field)(description = "The unique id of the article") id: Long,
                   @(ApiModelProperty@field)(description = "Available titles for the article") title: String,
                   @(ApiModelProperty@field)(description = "The content of the article in available languages") content: String,
                   @(ApiModelProperty@field)(description = "The language of the current concept") lanugage: String,
                   @(ApiModelProperty@field)(description = "All available languages of the current concept") supportedLanguages: Seq[String]
                  )
