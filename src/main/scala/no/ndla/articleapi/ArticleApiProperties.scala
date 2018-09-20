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
  val ApplicationName = "article-api"
  val Auth0LoginEndpoint = "https://ndla.eu.auth0.com/authorize"
  val RoleWithWriteAccess = "articles:write"
  val DraftRoleWithWriteAccess = "drafts:write"

  val SecretsFile = "article-api.secrets"

  val ApplicationPort: Int = propOrElse("APPLICATION_PORT", "80").toInt
  val ContactEmail = "christergundersen@ndla.no"
  val Environment: String = propOrElse("NDLA_ENVIRONMENT", "local")

  lazy val MetaUserName: String = prop(PropertyKeys.MetaUserNameKey)
  lazy val MetaPassword: String = prop(PropertyKeys.MetaPasswordKey)
  lazy val MetaResource: String = prop(PropertyKeys.MetaResourceKey)
  lazy val MetaServer: String = prop(PropertyKeys.MetaServerKey)
  lazy val MetaPort: Int = prop(PropertyKeys.MetaPortKey).toInt
  lazy val MetaSchema: String = prop(PropertyKeys.MetaSchemaKey)
  val MetaInitialConnections = 3
  val MetaMaxConnections = 20

  val AttachmentStorageName = s"$Environment.article-attachments.ndla"

  val SearchServer: String = propOrElse("SEARCH_SERVER", "http://search-article-api.ndla-local")
  val SearchRegion: String = propOrElse("SEARCH_REGION", "eu-central-1")
  val RunWithSignedSearchRequests: Boolean = propOrElse("RUN_WITH_SIGNED_SEARCH_REQUESTS", "true").toBoolean
  val ArticleSearchIndex: String = propOrElse("SEARCH_INDEX_NAME", "articles")
  val ConceptSearchIndex: String = propOrElse("CONCEPT_SEARCH_INDEX_NAME", "concepts")
  val ArticleSearchDocument = "article"
  val ConceptSearchDocument = "concept"
  val DefaultPageSize = 10
  val MaxPageSize = 100
  val IndexBulkSize = 200
  val ElasticSearchIndexMaxResultWindow = 10000

  val TopicAPIUrl = "http://api.topic.ndla.no/rest/v1/keywords/?filter[node]=ndlanode_"
  val MigrationHost: String = prop("MIGRATION_HOST")
  val MigrationUser: String = prop("MIGRATION_USER")
  val MigrationPassword: String = prop("MIGRATION_PASSWORD")

  val CorrelationIdKey = "correlationID"
  val CorrelationIdHeader = "X-Correlation-ID"
  val AudioHost: String = propOrElse("AUDIO_API_HOST", "audio-api.ndla-local")
  val ImageHost: String = propOrElse("IMAGE_API_HOST", "image-api.ndla-local")
  val DraftHost: String = propOrElse("DRAFT_API_HOST", "draft-api.ndla-local")
  val ApiClientsCacheAgeInMs: Long = 1000 * 60 * 60 // 1 hour caching

  val nodeTypeBegrep: String = "begrep"
  val nodeTypeVideo: String = "video"
  val nodeTypeH5P: String = "h5p_content"

  val supportedContentTypes =
    Set("fagstoff", "oppgave", "veiledning", "aktualitet", "emneartikkel", nodeTypeBegrep, nodeTypeVideo, nodeTypeH5P)

  val oldCreatorTypes = List("opphavsmann",
                             "fotograf",
                             "kunstner",
                             "forfatter",
                             "manusforfatter",
                             "innleser",
                             "oversetter",
                             "regissør",
                             "illustratør",
                             "medforfatter",
                             "komponist")

  val creatorTypes = List("originator",
                          "photographer",
                          "artist",
                          "writer",
                          "scriptwriter",
                          "reader",
                          "translator",
                          "director",
                          "illustrator",
                          "cowriter",
                          "composer")

  val oldProcessorTypes =
    List("bearbeider", "tilrettelegger", "redaksjonelt", "språklig", "ide", "sammenstiller", "korrektur")
  val processorTypes = List("processor", "facilitator", "editorial", "linguistic", "idea", "compiler", "correction")

  val oldRightsholderTypes = List("rettighetshaver", "forlag", "distributør", "leverandør")
  val rightsholderTypes = List("rightsholder", "publisher", "distributor", "supplier")
  val allowedAuthors: List[String] = creatorTypes ++ processorTypes ++ rightsholderTypes

  // When converting a content node, the converter may run several times over the content to make sure
  // everything is converted. This value defines a maximum number of times the converter runs on a node
  val maxConvertionRounds = 5

  lazy val Domain: String = Domains.get(Environment)

  val externalApiUrls = Map(
    ResourceType.Image.toString -> s"$Domain/image-api/v2/images",
    "raw-image" -> s"$Domain/image-api/raw/id",
    ResourceType.Audio.toString -> s"$Domain/audio-api/v1/audio",
    ResourceType.File.toString -> Domain
  )

  val NDLABrightcoveAccountId: String = prop("NDLA_BRIGHTCOVE_ACCOUNT_ID")
  val NDLABrightcovePlayerId: String = prop("NDLA_BRIGHTCOVE_PLAYER_ID")

  val H5PResizerScriptUrl = "//ndla.no/sites/all/modules/h5p/library/js/h5p-resizer.js"

  val NDLABrightcoveVideoScriptUrl =
    s"//players.brightcove.net/$NDLABrightcoveAccountId/${NDLABrightcovePlayerId}_default/index.min.js"
  val NRKVideoScriptUrl = Seq("//www.nrk.no/serum/latest/js/video_embed.js", "//nrk.no/serum/latest/js/video_embed.js")

  lazy val secrets: Map[String, Option[String]] = readSecrets(SecretsFile) match {
    case Success(values)    => values
    case Failure(exception) => throw new RuntimeException(s"Unable to load remote secrets from $SecretsFile", exception)
  }

  def booleanProp(key: String): Boolean = prop(key).toBoolean

  def prop(key: String): String = {
    propOrElse(key, throw new RuntimeException(s"Unable to load property $key"))
  }

  def propOrElse(key: String, default: => String): String = {
    secrets.get(key).flatten match {
      case Some(secret) => secret
      case None =>
        envOrNone(key) match {
          case Some(env) => env
          case None      => default
        }
    }
  }
}
