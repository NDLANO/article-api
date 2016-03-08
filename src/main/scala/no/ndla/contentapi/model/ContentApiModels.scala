package no.ndla.contentapi.model

import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Short summary of information about the content")
case class ContentSummary(
  @(ApiModelProperty @field)(description = "The unique id of the content") id:String,
  @(ApiModelProperty @field)(description = "The title of the content") title:String,
  @(ApiModelProperty @field)(description = "The full url to where the complete information about the content can be found") url:String,
  @(ApiModelProperty @field)(description = "Describes the license of the content") license:String
)

@ApiModel(description = "Information about the content")
case class ContentInformation(
  @(ApiModelProperty @field)(description = "The unique id of the content") id:String,
  @(ApiModelProperty @field)(description = "Available titles for the content") titles:List[ContentTitle],
  @(ApiModelProperty @field)(description = "The content in available languages") content: List[Content],
  @(ApiModelProperty @field)(description = "Describes the copyright information for the content") copyright:Copyright,
  @(ApiModelProperty @field)(description = "Searchable tags for the content") tags:List[ContentTag],
  @(ApiModelProperty @field)(description = "Required libraries in order to render the content") requiredLibraries:List[RequiredLibrary]
)

@ApiModel(description = "The content in the specified language")
case class Content(
  @(ApiModelProperty @field)(description = "The html content") content:String,
  @(ApiModelProperty @field)(description = "ISO 639-1 code that represents the language used in title") language:Option[String]
)

@ApiModel(description = "Description of a title")
case class ContentTitle(
  @(ApiModelProperty @field)(description = "The freetext title of the content") title:String,
  @(ApiModelProperty @field)(description = "ISO 639-1 code that represents the language used in title") language:Option[String]
)

@ApiModel(description = "Description of copyright information")
case class Copyright(
  @(ApiModelProperty @field)(description = "Describes the license of the content") license:License,
  @(ApiModelProperty @field)(description = "Reference to where the content is procured") origin:String,
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

@ApiModel(description = "Information about a library required to render the content")
case class RequiredLibrary(
  @(ApiModelProperty @field)(description = "The type of the library. E.g. CSS or JavaScript") mediaType:String,
  @(ApiModelProperty @field)(description = "The name of the library") name:String,
  @(ApiModelProperty @field)(description = "The full url to where the library can be downloaded") url:String
)
