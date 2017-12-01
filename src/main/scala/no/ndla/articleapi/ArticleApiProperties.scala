/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.validation.ResourceType
import no.ndla.network.secrets.PropertyKeys
import no.ndla.network.secrets.Secrets.readSecrets
import no.ndla.network.Domains

import scala.util.Properties._
import scala.util.{Failure, Success}

object ArticleApiProperties extends LazyLogging {
  val RoleWithWriteAccess = "articles:write"
  val SecretsFile = "article-api.secrets"

  val ApplicationPort = propOrElse("APPLICATION_PORT", "80").toInt
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

  val AttachmentStorageName = s"$Environment.article-attachments.ndla"

  val SearchServer = propOrElse("SEARCH_SERVER", "http://search-article-api.ndla-local")
  val SearchRegion = propOrElse("SEARCH_REGION", "eu-central-1")
  val RunWithSignedSearchRequests = propOrElse("RUN_WITH_SIGNED_SEARCH_REQUESTS", "true").toBoolean
  val ArticleSearchIndex = propOrElse("SEARCH_INDEX_NAME", "articles")
  val ConceptSearchIndex = propOrElse("CONCEPT_SEARCH_INDEX_NAME", "concepts")
  val ArticleSearchDocument = "article"
  val ConceptSearchDocument = "concept"
  val DefaultPageSize = 10
  val MaxPageSize = 100
  val IndexBulkSize = 200
  val ElasticSearchIndexMaxResultWindow = 10000

  val TopicAPIUrl = "http://api.topic.ndla.no/rest/v1/keywords/?filter[node]=ndlanode_"
  val MigrationHost = prop("MIGRATION_HOST")
  val MigrationUser = prop("MIGRATION_USER")
  val MigrationPassword = prop("MIGRATION_PASSWORD")

  val CorrelationIdKey = "correlationID"
  val CorrelationIdHeader = "X-Correlation-ID"
  val AudioHost = propOrElse("AUDIO_API_HOST", "audio-api.ndla-local")
  val ImageHost = propOrElse("IMAGE_API_HOST", "image-api.ndla-local")
  val DraftHost = propOrElse("DRAFT_API_HOST", "draft-api.ndla-local")
  val ApiClientsCacheAgeInMs: Long = 1000 * 60 * 60 // 1 hour caching

  val nodeTypeBegrep: String = "begrep"
  val nodeTypeVideo: String = "video"
  val nodeTypeH5P: String = "h5p_content"
  val supportedContentTypes = Set("fagstoff", "oppgave", "veiledning", "aktualitet", "emneartikkel", nodeTypeBegrep, nodeTypeVideo, nodeTypeH5P)

  val oldCreatorTypes = List("opphavsmann", "fotograf", "kunstner", "redaksjonelt", "forfatter", "manusforfatter", "innleser", "oversetter", "regissør", "illustratør", "medforfatter", "komponist")
  val creatorTypes = List("originator", "photographer", "artist", "editorial", "writer", "scriptwriter", "reader", "translator", "director", "illustrator", "cowriter", "composer")

  val oldProcessorTypes = List("bearbeider", "tilrettelegger", "redaksjonelt", "språklig", "ide", "sammenstiller", "korrektur")
  val processorTypes = List("processor", "facilitator", "editorial", "linguistic", "idea", "compiler", "correction")

  val oldRightsholderTypes = List("rettighetshaver", "forlag", "distributør", "leverandør")
  val rightsholderTypes = List("rightsholder", "publisher", "distributor", "supplier")
  val allowedAuthors = creatorTypes ++ processorTypes ++ rightsholderTypes

  // When converting a content node, the converter may run several times over the content to make sure
  // everything is converted. This value defines a maximum number of times the converter runs on a node
  val maxConvertionRounds = 5

  lazy val Domain = Domains.get(Environment)

  val externalApiUrls = Map(
    ResourceType.Image.toString -> s"$Domain/image-api/v2/images",
    "raw-image" -> s"$Domain/image-api/raw/id",
    ResourceType.Audio.toString -> s"$Domain/audio-api/v1/audio"
  )

  val NDLABrightcoveAccountId = prop("NDLA_BRIGHTCOVE_ACCOUNT_ID")
  val NDLABrightcovePlayerId = prop("NDLA_BRIGHTCOVE_PLAYER_ID")
  val EnableJoubelH5POembed = booleanProp("ENABLE_JOUBEL_H5P_OEMBED")

  val H5PResizerScriptUrl = "//ndla.no/sites/all/modules/h5p/library/js/h5p-resizer.js"
  val H5PHost = "h5p-test.ndla.no/v1/ndla" //TODO: prop("NDLA_H5P_HOST") or something
  val NDLABrightcoveVideoScriptUrl = s"//players.brightcove.net/$NDLABrightcoveAccountId/${NDLABrightcovePlayerId}_default/index.min.js"
  val NRKVideoScriptUrl = Seq("//www.nrk.no/serum/latest/js/video_embed.js", "//nrk.no/serum/latest/js/video_embed.js")

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
