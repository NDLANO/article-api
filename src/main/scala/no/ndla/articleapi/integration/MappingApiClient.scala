/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.integration

import no.ndla.articleapi.ArticleApiProperties
import no.ndla.articleapi.caching.Memoize
import no.ndla.network.NdlaClient

import scalaj.http.Http

trait MappingApiClient {
  this: NdlaClient =>
  val mappingApiClient: MappingApiClient

  class MappingApiClient {

    private val allLanguageMappingsEndpoint = s"http://${ArticleApiProperties.MappingHost}/iso639"
    private val allLicenseDefinitionsEndpoint = s"http://${ArticleApiProperties.MappingHost}/licenses"

    def getLicenseDefinition(licenseName: String): Option[LicenseDefinition] = {
      getLicenseDefinitions().find(_.license == licenseName).map(l => LicenseDefinition(l.license, l.description, l.url))
    }

    def get6391CodeFor6392Code(languageCode6392: String): Option[String] = getLanguageMapping().find(_._1 == languageCode6392).map(_._2)

    def languageCodeSupported(languageCode: String): Boolean = getLanguageMapping().exists(_._1 == languageCode)

    private val getLicenseDefinitions = Memoize[Seq[LicenseDefinition]](() =>
      ndlaClient.fetch[Seq[LicenseDefinition]](Http(allLicenseDefinitionsEndpoint)).get)

    private val getLanguageMapping = Memoize[Map[String, String]](() =>
      ndlaClient.fetch[Map[String, String]](Http(allLanguageMappingsEndpoint)).get)
  }
}

case class LicenseDefinition(license: String, description: String, url: Option[String])

