/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.integration

import no.ndla.articleapi.ArticleApiProperties
import no.ndla.network.NdlaClient

import scala.util.{Failure, Success, Try}
import scalaj.http.Http

trait AudioApiClient {
  this: NdlaClient =>
  val audioApiClient: AudioApiClient

  class AudioApiClient {
    private val AudioMetaInternEndpoint = s"http://${ArticleApiProperties.AudioHost}/intern"
    private val AudioMetaFromExternalIdEndpoint = s"$AudioMetaInternEndpoint/:external_id"
    private val ImportAudioEndpoint = s"$AudioMetaInternEndpoint/import/:external_id"

    def getOrImportAudio(externalId: String): Option[Long] = {
      getAudioFromExternalId(externalId) match {
        case Failure(_) => importAudio(externalId).toOption
        case Success(audio) => Some(audio)
      }
    }

    def getAudioFromExternalId(externalId: String): Try[Long] = {
      val request = Http(AudioMetaFromExternalIdEndpoint.replace(":external_id", externalId))
      ndlaClient.fetch[AudioApiMetaInformation](request).map(_.id)
    }

    def importAudio(externalId: String): Try[Long] = {
      val request = Http(ImportAudioEndpoint.replace(":external_id", externalId)).postForm
      ndlaClient.fetch[AudioApiMetaInformation](request).map(_.id)
    }

  }
}

case class AudioApiMetaInformation(id: Long)
