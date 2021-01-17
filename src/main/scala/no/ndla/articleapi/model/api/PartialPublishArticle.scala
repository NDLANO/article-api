/*
 * Part of NDLA article-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

// format: off
@ApiModel(description = "Partial data about article to publish independently")
case class PartialPublishArticle(
    @(ApiModelProperty @field)(description = "A list of codes from GREP API connected to the article") grepCodes: Option[Seq[String]],
    @(ApiModelProperty @field)(description = "The name of the license") license: Option[String],
    @(ApiModelProperty @field)(description = "Meta description for the article") metaDescription: Option[String],
    @(ApiModelProperty @field)(description = "A comma separated list of tags") tags: Option[Seq[String]],
)
