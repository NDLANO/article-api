/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.api

import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Description of a visual element")
case class VisualElement(
    @(ApiModelProperty @field)(description =
      "Html containing the visual element. May contain any legal html element, including the embed-tag") visualElement: String,
    @(ApiModelProperty @field)(description =
      "The ISO 639-1 language code describing which article translation this visual element belongs to") language: String)
