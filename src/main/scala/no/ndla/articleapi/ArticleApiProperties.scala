/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.service.converters.ResourceType
import no.ndla.network.secrets.PropertyKeys
import no.ndla.network.secrets.Secrets.readSecrets
import no.ndla.network.Domains

import scala.util.Properties._
import scala.util.{Failure, Success}

object ArticleApiProperties extends LazyLogging {
  val SecretsFile = "article-api.secrets"

  val ApplicationPort = 80
  val ContactEmail = "christergundersen@ndla.no"
  val Environment = propOrElse("NDLA_ENVIRONMENT", "local")

  lazy val MetaUserName = prop(PropertyKeys.MetaUserNameKey)
  lazy val MetaPassword = prop(PropertyKeys.MetaPasswordKey)
  lazy val MetaResource = prop(PropertyKeys.MetaResourceKey)
  lazy val MetaServer = prop(PropertyKeys.MetaServerKey)
  lazy val MetaPort = prop(PropertyKeys.MetaPortKey).toInt
  lazy val MetaSchema = prop(PropertyKeys.MetaSchemaKey)
  val MetaInitialConnections = 3
  val MetaMaxConnections = 20

  val AttachmentStorageName = s"$Environment.attachments.ndla"

  val SearchServer = propOrElse("SEARCH_SERVER", "http://search-article-api.ndla-local")
  val SearchRegion = propOrElse("SEARCH_REGION", "eu-central-1")
  val RunWithSignedSearchRequests = propOrElse("RUN_WITH_SIGNED_SEARCH_REQUESTS", "true").toBoolean
  val SearchIndex = "articles"
  val SearchDocument = "article"
  val DefaultPageSize = 10
  val MaxPageSize = 100
  val IndexBulkSize = 1000

  val TopicAPIUrl = "http://api.topic.ndla.no/rest/v1/keywords/?filter[node]=ndlanode_"
  val MigrationHost = prop("MIGRATION_HOST")
  val MigrationUser = prop("MIGRATION_USER")
  val MigrationPassword = prop("MIGRATION_PASSWORD")

  val CorrelationIdKey = "correlationID"
  val CorrelationIdHeader = "X-Correlation-ID"
  val AudioHost = "audio-api.ndla-local"
  val MappingHost = "mapping-api.ndla-local"
  val internalImageApiUrl = "image-api.ndla-local"
  val ApiClientsCacheAgeInMs: Long = 1000 * 60 * 60 // 1 hour caching

  val supportedContentTypes = Set("fagstoff", "oppgave", "veiledning", "aktualitet", "emneartikkel")


  // When converting a content node, the converter may run several times over the content to make sure
  // everything is converted. This value defines a maximum number of times the converter runs on a node
  val maxConvertionRounds = 5

  lazy val Domain = Domains.get(Environment)

  val externalApiUrls = Map(
    ResourceType.Image.toString -> s"$Domain/image-api/v1/images",
    ResourceType.Audio.toString -> s"$Domain/audio-api/v1/audio"
  )

  val resourceHtmlEmbedTag = "embed"

  val NDLABrightcoveAccountId = prop("NDLA_BRIGHTCOVE_ACCOUNT_ID")
  val NDLABrightcovePlayerId = prop("NDLA_BRIGHTCOVE_PLAYER_ID")
  val EnableJoubelH5POembed = booleanProp("ENABLE_JOUBEL_H5P_OEMBED")

  val H5PResizerScriptUrl = "http://ndla.no/sites/all/modules/h5p/library/js/h5p-resizer.js"
  val NDLABrightcoveVideoScriptUrl = s"http://players.brightcove.net/$NDLABrightcoveAccountId/${NDLABrightcovePlayerId}_default/index.min.js"
  val NRKVideoScriptUrl = "http://www.nrk.no/serum/latest/js/video_embed.js"

  lazy val secrets = readSecrets(SecretsFile) match {
     case Success(values) => values
     case Failure(exception) => throw new RuntimeException(s"Unable to load remote secrets from $SecretsFile", exception)
   }

  def booleanProp(key: String) = prop(key).toBoolean

  def prop(key: String): String = {
    propOrElse(key, throw new RuntimeException(s"Unable to load property $key"))
  }

  def propOrElse(key: String, default: => String): String = {
    secrets.get(key).flatten match {
      case Some(secret) => secret
      case None =>
        envOrNone(key) match {
          case Some(env) => env
          case None => default
        }
    }
  }
}
