/*
 * Part of NDLA article-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.model.api
import org.scalatra.swagger.annotations.ApiModelProperty

import scala.annotation.meta.field

case class NewArticleMetaImage(
    @(ApiModelProperty @field)(description = "The image-api id of the meta image") id: String,
    @(ApiModelProperty @field)(description = "The alt text of the meta image") alt: String)
