/*
 * Part of NDLA Content-API. API for searching and downloading content from NDLA.
 * Copyright (C) 2015 NDLA
 *
 * See LICENSE
 */
package no.ndla.contentapi.model

import java.text.SimpleDateFormat
import java.util.Date

import no.ndla.contentapi.ContentApiProperties


object Error {
  val GENERIC = "1"
  val NOT_FOUND = "2"
  val INDEX_MISSING = "3"

  val GenericError = Error(GENERIC, s"Ooops. Something we didn't anticipate occured. We have logged the error, and will look into it. But feel free to contact ${ContentApiProperties.ContactEmail} if the error persists.")
  val IndexMissingError = Error(INDEX_MISSING, s"Ooops. Our search index is not available at the moment, but we are trying to recreate it. Please try again in a few minutes. Feel free to contact ${ContentApiProperties.ContactEmail} if the error persists.")
}

case class Error(code:String, description:String, occuredAt:String = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()))