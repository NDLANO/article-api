/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.model

import java.util.Date

import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Short summary of information about the article")
case class ArticleSummary(
  @(ApiModelProperty @field)(description = "The unique id of the article") id: String,
  @(ApiModelProperty @field)(description = "The title of the article") titles: Seq[ArticleTitle],
  @(ApiModelProperty @field)(description = "The full url to where the complete information about the article can be found") url: String,
  @(ApiModelProperty @field)(description = "Describes the license of the article") license: String
)

@ApiModel(description = "Information about the article")
case class ArticleInformation(@(ApiModelProperty@field)(description = "The unique id of the article") id: String,
                              @(ApiModelProperty@field)(description = "Available titles for the article") titles: Seq[ArticleTitle],
                              @(ApiModelProperty@field)(description = "The article in available languages") article: Seq[Article],
                              @(ApiModelProperty@field)(description = "Describes the copyright information for the article") copyright: Copyright,
                              @(ApiModelProperty@field)(description = "Searchable tags for the article") tags: Seq[ArticleTag],
                              @(ApiModelProperty@field)(description = "Required libraries in order to render the article") requiredLibraries: Seq[RequiredLibrary],
                              @(ApiModelProperty@field)(description = "A visual element article") visualElement: Seq[VisualElement],
                              @(ApiModelProperty@field)(description = "An introduction for the article") introduction: Seq[ArticleIntroduction],
                              @(ApiModelProperty@field)(description = "When the article was created") created: Date,
                              @(ApiModelProperty@field)(description = "When the article was last updated") updated: Date,
                              @(ApiModelProperty@field)(description = "The type of learning resource") contentType: String)

@ApiModel(description = "The article in the specified language")
case class Article(@(ApiModelProperty@field)(description = "The html article") article: String,
                   @(ApiModelProperty@field)(description = "Foot notes referred to within the html article") footNotes: Option[Map[String, FootNoteItem]],
                   @(ApiModelProperty@field)(description = "ISO 639-1 code that represents the language used in title") language: Option[String])

@ApiModel(description = "Description of a title")
case class ArticleTitle(@(ApiModelProperty@field)(description = "The freetext title of the article") title: String,
                        @(ApiModelProperty@field)(description = "ISO 639-1 code that represents the language used in title") language: Option[String])

@ApiModel(description = "Description of copyright information")
case class Copyright(@(ApiModelProperty@field)(description = "Describes the license of the article") license: License,
                     @(ApiModelProperty@field)(description = "Reference to where the article is procured") origin: String,
                     @(ApiModelProperty@field)(description = "List of authors") authors: Seq[Author])

@ApiModel(description = "Description of the tags of the article")
case class ArticleTag(@(ApiModelProperty@field)(description = "The searchable tag.") tags: Seq[String],
                      @(ApiModelProperty@field)(description = "ISO 639-1 code that represents the language used in tag") language: Option[String])

@ApiModel(description = "Description of license information")
case class License(@(ApiModelProperty@field)(description = "The name of the license") license: String,
                   @(ApiModelProperty@field)(description = "Description of the license") description: String,
                   @(ApiModelProperty@field)(description = "Url to where the license can be found") url: Option[String])

@ApiModel(description = "Information about an author")
case class Author(@(ApiModelProperty@field)(description = "The description of the author. Eg. Photographer or Supplier") `type`: String,
                  @(ApiModelProperty@field)(description = "The name of the of the author") name: String)

@ApiModel(description = "Information about a library required to render the article")
case class RequiredLibrary(@(ApiModelProperty@field)(description = "The type of the library. E.g. CSS or JavaScript") mediaType: String,
                           @(ApiModelProperty@field)(description = "The name of the library") name: String,
                           @(ApiModelProperty@field)(description = "The full url to where the library can be downloaded") url: String)

@ApiModel(description = "Description of a foot note item")
case class FootNoteItem(@(ApiModelProperty@field)(description = "Title of the item") title: String,
                        @(ApiModelProperty@field)(description = "Type of the item (book, ...)") `type`: String,
                        @(ApiModelProperty@field)(description = "Year of publishment") year: String,
                        @(ApiModelProperty@field)(description = "Edition of item") edition: String,
                        @(ApiModelProperty@field)(description = "Name of the publisher") publisher: String,
                        @(ApiModelProperty@field)(description = "Names of the authors") authors: Seq[String])

@ApiModel(description = "Information about search-results")
case class SearchResult(@(ApiModelProperty@field)(description = "The total number of articles matching this query") totalCount: Long,
                        @(ApiModelProperty@field)(description = "For which page results are shown from") page: Int,
                        @(ApiModelProperty@field)(description = "The number of results per page") pageSize: Int,
                        @(ApiModelProperty@field)(description = "The search results") results: Seq[ArticleSummary])

@ApiModel(description = "Description of a foot note item")
case class VisualElement(@(ApiModelProperty@field)(description = "The resource identifier") resource: String,
                         @(ApiModelProperty@field)(description = "The type of visual element (can be an image, a video or a presentation)") `type`: String,
                         @(ApiModelProperty@field)(description = "The ISO 639-1 language code describing which article translation this visual element belongs to") language: Option[String])

@ApiModel(description = "Description of the article introduction")
case class ArticleIntroduction(@(ApiModelProperty@field)(description = "The introduction content") introduction: String,
                               @(ApiModelProperty@field)(description = "An image complementing the introduction text") image: Option[String],
                               @(ApiModelProperty@field)(description = "A boolean describing whether to display the introduction in the article or not") displayIngress: Boolean,
                               @(ApiModelProperty@field)(description = "The ISO 639-1 language code describing which article translation this introduction belongs to") language: Option[String])

object FootNoteItem {
  def apply(biblio: Biblio, authors: Seq[BiblioAuthor]): FootNoteItem =
    FootNoteItem(biblio.title, biblio.bibType, biblio.year, biblio.edition, biblio.publisher, authors.map(x => x.name))
}
