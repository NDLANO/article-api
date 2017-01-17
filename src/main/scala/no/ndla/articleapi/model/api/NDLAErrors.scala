/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.model.api

import java.util.Date
import scala.annotation.meta.field

import no.ndla.articleapi.ArticleApiProperties
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

@ApiModel(description = "Information about an error")
case class Error(@(ApiModelProperty@field)(description = "Code stating the type of error") code: String = Error.GENERIC,
                 @(ApiModelProperty@field)(description = "Description of the error") description: String = Error.GENERIC_DESCRIPTION,
                 @(ApiModelProperty@field)(description = "When the error occured") occuredAt: Date = new Date())

object Error {
  val GENERIC = "1"
  val NOT_FOUND = "2"
  val INDEX_MISSING = "3"
  val VALIDATION = "4"

  val VALIDATION_DESCRIPTION = "Validation Error"
  val GENERIC_DESCRIPTION = s"Ooops. Something we didn't anticipate occured. We have logged the error, and will look into it. But feel free to contact ${ArticleApiProperties.ContactEmail} if the error persists."
  val INDEX_MISSING_DESCRIPTION = s"Ooops. Our search index is not available at the moment, but we are trying to recreate it. Please try again in a few minutes. Feel free to contact ${ArticleApiProperties.ContactEmail} if the error persists."

  val GenericError = Error(GENERIC, GENERIC_DESCRIPTION)
  val IndexMissingError = Error(INDEX_MISSING, INDEX_MISSING_DESCRIPTION)
}

case class NodeNotFoundException(message: String) extends Exception(message)
class ValidationException(message: String = "Validation Error", val errors: Seq[ValidationMessage]) extends RuntimeException(message)
