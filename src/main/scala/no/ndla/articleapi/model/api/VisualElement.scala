/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.model.api

import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Description of a foot note item")
case class VisualElement(@(ApiModelProperty@field)(description = "The resource identifier") content: String,
                         @(ApiModelProperty@field)(description = "The ISO 639-1 language code describing which article translation this visual element belongs to") language: Option[String])




