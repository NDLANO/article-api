/*
 * Part of NDLA content_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.integration

import no.ndla.articleapi.ContentApiProperties
import no.ndla.articleapi.caching.Memoize
import no.ndla.network.NdlaClient

import scala.util.{Failure, Success}
import scalaj.http.Http

trait MappingApiClient {
  this: NdlaClient =>
  val mappingApiClient: MappingApiClient

  class MappingApiClient {

    private val allLanguageMappingsEndpoint = s"http://${ContentApiProperties.MappingHost}/iso639"

    def get6391CodeFor6392Code(languageCode6392: String): Option[String] = getLanguageMapping().find(_._1 == languageCode6392).map(_._2)

    def languageCodeSupported(languageCode: String): Boolean = getLanguageMapping().exists(_._1 == languageCode)

    private val getLanguageMapping = Memoize[Map[String, String]](ContentApiProperties.IsoMappingCacheAgeInMs, () => {
      ndlaClient.fetch[Map[String, String]](Http(allLanguageMappingsEndpoint)) match {
        case Success(map) => map
        case Failure(ex) => throw ex
      }
    })
  }
}

