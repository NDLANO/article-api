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
  val GENERIC = "GENERIC"
  val NOT_FOUND = "NOT_FOUND"
  val INDEX_MISSING = "INDEX_MISSING"
  val VALIDATION = "VALIDATION"
  val RESOURCE_OUTDATED = "RESOURCE_OUTDATED"
  val ACCESS_DENIED = "ACCESS DENIED"
  val WINDOW_TOO_LARGE = "RESULT_WINDOW_TOO_LARGE"

  val VALIDATION_DESCRIPTION = "Validation Error"
  val GENERIC_DESCRIPTION = s"Ooops. Something we didn't anticipate occured. We have logged the error, and will look into it. But feel free to contact ${ArticleApiProperties.ContactEmail} if the error persists."
  val INDEX_MISSING_DESCRIPTION = s"Ooops. Our search index is not available at the moment, but we are trying to recreate it. Please try again in a few minutes. Feel free to contact ${ArticleApiProperties.ContactEmail} if the error persists."
  val RESOURCE_OUTDATED_DESCRIPTION = "The resource is outdated. Please try fetching before submitting again."
  val WINDOW_TOO_LARGE_DESCRIPTION = s"The result window is too large. Fetching pages above ${ArticleApiProperties.ElasticSearchIndexMaxResultWindow} results are unsupported."

  val GenericError = Error(GENERIC, GENERIC_DESCRIPTION)
  val IndexMissingError = Error(INDEX_MISSING, INDEX_MISSING_DESCRIPTION)
}

case class NotFoundException(message: String) extends RuntimeException(message)
case class ImportException(message: String) extends RuntimeException(message)

class AccessDeniedException(message: String) extends RuntimeException(message)
class ImportExceptions(val message: String, val errors: Seq[Throwable]) extends RuntimeException(message)
class OptimisticLockException(message: String = Error.RESOURCE_OUTDATED_DESCRIPTION) extends RuntimeException(message)
class ConfigurationException(message: String) extends RuntimeException(message)
class ResultWindowTooLargeException(message: String = Error.WINDOW_TOO_LARGE_DESCRIPTION) extends RuntimeException(message)
