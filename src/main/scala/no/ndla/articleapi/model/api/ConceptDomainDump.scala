/*
 * Part of NDLA article-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}
import no.ndla.articleapi.model.domain

import scala.annotation.meta.field

@ApiModel(description = "Information about concepts")
case class ConceptDomainDump(
    @(ApiModelProperty @field)(description = "The total number of concepts in the database") totalCount: Long,
    @(ApiModelProperty @field)(description = "For which page results are shown from") page: Int,
    @(ApiModelProperty @field)(description = "The number of results per page") pageSize: Int,
    @(ApiModelProperty @field)(description = "The search results") results: Seq[domain.Concept])
