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

@ApiModel(description = "Meta information about the content")
case class ContentMetaInformation(
  @(ApiModelProperty @field)(description = "The unique id of the content") id:String,
  @(ApiModelProperty @field)(description = "Available titles for the image") titles:List[ContentTitle],
  @(ApiModelProperty @field)(description = "The content") content: String,
  @(ApiModelProperty @field)(description = "Describes the copyright information for the image") copyright:Copyright,
  @(ApiModelProperty @field)(description = "Searchable tags for the image") tags:List[ContentTag]
)

@ApiModel(description = "Description of a title")
case class ContentTitle(
  @(ApiModelProperty @field)(description = "The freetext title of the image") title:String,
  @(ApiModelProperty @field)(description = "ISO 639-1 code that represents the language used in title") language:Option[String]
)

@ApiModel(description = "Description of copyright information")
case class Copyright(
  @(ApiModelProperty @field)(description = "Describes the license of the image") license:License,
  @(ApiModelProperty @field)(description = "Reference to where the image is procured") origin:String,
  @(ApiModelProperty @field)(description = "List of authors") authors:List[Author]
)

@ApiModel(description = "Description of the tags of the content")
case class ContentTag(
  @(ApiModelProperty @field)(description = "The searchable tag.") tag:String,
  @(ApiModelProperty @field)(description = "ISO 639-1 code that represents the language used in tag") language:Option[String]
)

@ApiModel(description = "Description of license information")
case class License(
  @(ApiModelProperty @field)(description = "The name of the license") license:String,
  @(ApiModelProperty @field)(description = "Description of the license") description:String,
  @(ApiModelProperty @field)(description = "Url to where the license can be found") url:Option[String]
)

@ApiModel(description = "Information about an author")
case class Author(
  @(ApiModelProperty @field)(description = "The description of the author. Eg. Photographer or Supplier") `type`:String,
  @(ApiModelProperty @field)(description = "The name of the of the author") name:String
)
