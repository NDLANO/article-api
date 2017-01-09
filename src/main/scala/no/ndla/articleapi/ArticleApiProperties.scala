/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.network.secrets.PropertyKeys
import no.ndla.network.secrets.Secrets.readSecrets

import scala.util.Properties._
import scala.util.{Failure, Success}

object ArticleApiProperties extends LazyLogging {
  val SecretsFile = "article-api.secrets"

  val ApplicationPort = 80
  val ContactEmail = "christergundersen@ndla.no"
  val Environment = propOrElse("NDLA_ENVIRONMENT", "local")

  val EnableJoubelH5POembed = booleanProp("ENABLE_JOUBEL_H5P_OEMBED")

  val MetaUserName = prop(PropertyKeys.MetaUserNameKey)
  val MetaPassword = prop(PropertyKeys.MetaPasswordKey)
  val MetaResource = prop(PropertyKeys.MetaResourceKey)
  val MetaServer = prop(PropertyKeys.MetaServerKey)
  val MetaPort = prop(PropertyKeys.MetaPortKey).toInt
  val MetaSchema = prop(PropertyKeys.MetaSchemaKey)
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

  val NDLABrightcoveAccountId = prop("NDLA_BRIGHTCOVE_ACCOUNT_ID")
  val NDLABrightcovePlayerId = prop("NDLA_BRIGHTCOVE_PLAYER_ID")

  // When converting a content node, the converter may run several times over the content to make sure
  // everything is converted. This value defines a maximum number of times the converter runs on a node
  val maxConvertionRounds = 5

  lazy val Domain = Map(
    "local" -> "http://localhost",
    "prod" -> "http://api.ndla.no"
  ).getOrElse(Environment, s"http://api.$Environment.ndla.no")

  val externalApiUrls = Map(
    "image" -> s"$Domain/image-api/v1/images",
    "audio" -> s"$Domain/audio-api/v1/audio"
  )

  val resourceHtmlEmbedTag = "embed"

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
