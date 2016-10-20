/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.api

import no.ndla.articleapi.model.domain.{Biblio, BiblioAuthor}
import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Description of a foot note item")
case class FootNoteItem(@(ApiModelProperty@field)(description = "Title of the item") title: String,
                        @(ApiModelProperty@field)(description = "Type of the item (book, ...)") `type`: String,
                        @(ApiModelProperty@field)(description = "Year of publishment") year: String,
                        @(ApiModelProperty@field)(description = "Edition of item") edition: String,
                        @(ApiModelProperty@field)(description = "Name of the publisher") publisher: String,
                        @(ApiModelProperty@field)(description = "Names of the authors") authors: Seq[String])

object FootNoteItem {
  def apply(biblio: Biblio, authors: Seq[BiblioAuthor]): FootNoteItem =
    FootNoteItem(biblio.title, biblio.bibType, biblio.year, biblio.edition, biblio.publisher, authors.map(x => x.name))
}
