/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Meta description of the article")
case class ArticleMetaDescription(
    @(ApiModelProperty @field)(description = "The meta description") metaDescription: String,
    @(ApiModelProperty @field)(description =
      "The ISO 639-1 language code describing which article translation this meta description belongs to") language: String)
