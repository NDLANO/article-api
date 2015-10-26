package no.ndla.contentapi.model

import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Short summary of meta information about the content")
case class ContentMetaSummary(
  @(ApiModelProperty @field)(description = "The unique id of the content") id:String,
  @(ApiModelProperty @field)(description = "The title of the content") title:String,
  @(ApiModelProperty @field)(description = "The full url to where the complete metainformation about the content can be found") metaUrl:String,
  @(ApiModelProperty @field)(description = "Describes the license of the content") license:String
)
