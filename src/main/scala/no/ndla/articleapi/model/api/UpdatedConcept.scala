/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Information about the concept")
case class UpdatedConcept(@(ApiModelProperty@field)(description = "The language of this concept") language: String,
                          @(ApiModelProperty@field)(description = "Available titles for the concept") title: Option[String],
                          @(ApiModelProperty@field)(description = "The content of the concept") content: Option[String],
                          @(ApiModelProperty@field)(description = "Describes the copyright information for the concept") copyright: Option[Copyright]
                         )
