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

@ApiModel(description = "Information about search-results")
case class ConceptSearchResult(
    @(ApiModelProperty @field)(description = "The total number of articles matching this query") totalCount: Long,
    @(ApiModelProperty @field)(description = "For which page results are shown from") page: Option[Int],
    @(ApiModelProperty @field)(description = "The number of results per page") pageSize: Int,
    @(ApiModelProperty @field)(description = "The chosen search language") language: String,
    @(ApiModelProperty @field)(description = "The search results") results: Seq[ConceptSummary])
