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

@ApiModel(description = "Description of copyright information")
case class Copyright(@(ApiModelProperty@field)(description = "Describes the license of the article") license: License,
                     @(ApiModelProperty@field)(description = "Reference to where the article is procured") origin: String,
                     @(ApiModelProperty@field)(description = "List of authors") authors: Seq[Author])
