/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.api

import java.util.Date

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Description of copyright information")
case class Copyright(@(ApiModelProperty@field)(description = "Describes the license of the article") license: License,
                     @(ApiModelProperty@field)(description = "Reference to where the article is procured") origin: String,
                     @(ApiModelProperty@field)(description = "List of creators") creators: Seq[Author],
                     @(ApiModelProperty@field)(description = "List of processors") processors: Seq[Author],
                     @(ApiModelProperty@field)(description = "List of rightsholders") rightsholders: Seq[Author],
                     @(ApiModelProperty@field)(description = "Date from which the copyright is valid") validFrom: Option[Date],
                     @(ApiModelProperty@field)(description = "Date to which the copyright is valid") validTo: Option[Date])
