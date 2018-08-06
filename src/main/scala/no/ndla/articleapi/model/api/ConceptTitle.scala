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
case class ConceptTitle(@(ApiModelProperty @field)(description = "The title of this concept") title: String,
                        @(ApiModelProperty @field)(description = "The language of this concept") language: String)
