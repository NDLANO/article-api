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

import scala.collection.mutable
import scala.io.Source
import scala.util.{Properties, Success, Try}

object ArticleApiProperties extends LazyLogging {

  var ArticleApiProps: mutable.Map[String, Option[String]] = mutable.HashMap()

  lazy val ApplicationPort = 80
  lazy val ContactEmail = "christergundersen@ndla.no"

  lazy val MetaUserName = get(PropertyKeys.MetaUserNameKey)
  lazy val MetaPassword = get(PropertyKeys.MetaPasswordKey)
  lazy val MetaResource = get(PropertyKeys.MetaResourceKey)
  lazy val MetaServer = get(PropertyKeys.MetaServerKey)
  lazy val MetaPort = getInt(PropertyKeys.MetaPortKey)
  lazy val MetaSchema = get(PropertyKeys.MetaSchemaKey)
  lazy val MetaInitialConnections = 3
  lazy val MetaMaxConnections = 20

  lazy val AttachmentStorageName = get("NDLA_ENVIRONMENT") + ".attachments.ndla"

  lazy val SearchServer = getOrElse("SEARCH_SERVER", "http://search-article-api.ndla-local")
  lazy val SearchRegion = getOrElse("SEARCH_REGION", "eu-central-1")
  lazy val SearchIndex = "articles"
  lazy val SearchDocument = "article"
  lazy val DefaultPageSize: Int = 10
  lazy val MaxPageSize: Int = 100
  lazy val IndexBulkSize = 1000
  lazy val RunWithSignedSearchRequests = getOrElse("RUN_WITH_SIGNED_SEARCH_REQUESTS", "true").toBoolean

  lazy val TopicAPIUrl = get("TOPIC_API_URL")
  lazy val MigrationHost = get("MIGRATION_HOST")
  lazy val MigrationUser = get("MIGRATION_USER")
  lazy val MigrationPassword = get("MIGRATION_PASSWORD")

  val CorrelationIdKey = "correlationID"
  val CorrelationIdHeader = "X-Correlation-ID"
  val AudioHost = "audio-api.ndla-local"
  val MappingHost = "mapping-api.ndla-local"
  val internalImageApiUrl = "image-api.ndla-local"
  val ApiClientsCacheAgeInMs: Long = 1000 * 60 * 60  // 1 hour caching

  lazy val NDLABrightcoveAccountId = get("NDLA_BRIGHTCOVE_ACCOUNT_ID")
  lazy val NDLABrightcovePlayerId = get("NDLA_BRIGHTCOVE_PLAYER_ID")

  // When converting a content node, the converter may run several times over the content to make sure
  // everything is converted. This value defines a maximum number of times the converter runs on a node
  val maxConvertionRounds = 5

  lazy val Environment = get("NDLA_ENVIRONMENT")
  lazy val Domain = getDomain
  lazy val externalImageApiUrl = s"$Domain/images"
  lazy val externalAudioApiUrl = s"$Domain/audio"

  val resourceHtmlEmbedTag = "embed"

  def setProperties(properties: Map[String, Option[String]]) = {
    Success(properties.foreach(prop => ArticleApiProps.put(prop._1, prop._2)))
  }

  private def getOrElse(envKey: String, defaultValue: String) = {
    ArticleApiProps.get(envKey).flatten match {
      case Some(value) => value
      case None => defaultValue
    }
  }

  private def getDomain: String = {
    Map("local" -> "http://localhost",
      "prod" -> "http://api.ndla.no"
    ).getOrElse(Environment, s"http://api.$Environment.ndla.no")
  }

  def get(envKey: String): String = {
    ArticleApiProps.get(envKey).flatten match {
      case Some(value) => value
      case None => throw new NoSuchFieldError(s"Missing environment variable $envKey")
    }
  }

  def getInt(envKey: String): Integer = {
    get(envKey).toInt
  }

  private def getBoolean(envKey: String): Boolean = {
    get(envKey).toBoolean
  }
}

object PropertiesLoader extends LazyLogging {
  val EnvironmentFile = "/article-api.env"

  def readPropertyFile() = {
    Try(Source.fromInputStream(getClass.getResourceAsStream(EnvironmentFile)).getLines().map(key => key -> Properties.envOrNone(key)).toMap)
  }

  def load() = {
    val verification = for {
      file <- readPropertyFile()
      secrets <- readSecrets("article_api.secrets")
      didSetProperties <- ArticleApiProperties.setProperties(file ++ secrets)
    } yield didSetProperties

    if (verification.isFailure) {
      logger.error("Unable to load properties", verification.failed.get)
      System.exit(1)
    }
  }
}
