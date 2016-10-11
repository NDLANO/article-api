/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi

import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable
import scala.io.Source

object ArticleApiProperties extends LazyLogging {
  var ContentApiProps: mutable.Map[String, Option[String]] = mutable.HashMap()

  lazy val ApplicationPort = getInt("APPLICATION_PORT")

  // When converting a content node, the converter may run several times over the content to make sure
  // everything is converted. This value defines a maximum number of times the converter runs on a node
  val maxConvertionRounds = 5

  val CorrelationIdHeader = "X-Correlation-ID"
  val CorrelationIdKey = "correlationID"

  lazy val NDLABrightcoveAccountId = get("NDLA_BRIGHTCOVE_ACCOUNT_ID")
  lazy val NDLABrightcovePlayerId = get("NDLA_BRIGHTCOVE_PLAYER_ID")

  lazy val ContactEmail = get("CONTACT_EMAIL")
  lazy val HostAddr = get("HOST_ADDR")
  lazy val Domains = get("DOMAINS").split(",") ++ Array(HostAddr)

  val audioStorageDirectory = "audio"

  lazy val imageApiBaseUrl = get("IMAGE_API_BASE_URL")
  val imageApiInternEndpointURLSuffix = "intern"
  val imageApiImportImageURL = s"$imageApiInternEndpointURLSuffix/import"
  val imageApiGetByExternalIdURL = s"$imageApiInternEndpointURLSuffix/extern"

  lazy val imageApiUrl = get("IMAGE_API_URL")

  val ndlaBaseHost = "http://ndla.no/"

  val SearchHost = "search-engine"
  lazy val SearchServer = get("SEARCH_SERVER")
  lazy val SearchRegion = get("SEARCH_REGION")
  lazy val RunWithSignedSearchRequests = getBoolean("RUN_WITH_SIGNED_SEARCH_REQUESTS")
  lazy val SearchIndex = get("SEARCH_INDEX")
  lazy val SearchDocument = get("SEARCH_DOCUMENT")
  lazy val DefaultPageSize: Int = getInt("SEARCH_DEFAULT_PAGE_SIZE")
  lazy val MaxPageSize: Int = getInt("SEARCH_MAX_PAGE_SIZE")
  lazy val IndexBulkSize = getInt("INDEX_BULK_SIZE")

  lazy val AmazonBaseUrl = get("AMAZON_BASE_URL")
  lazy val StorageName = get("STORAGE_NAME")
  lazy val StorageAccessKey = get("STORAGE_ACCESS_KEY")
  lazy val StorageSecretKey = get("STORAGE_SECRET_KEY")
  lazy val amazonUrlPrefix = s"$AmazonBaseUrl/$StorageName"

  lazy val CMHost = get("CM_HOST")
  lazy val CMPort = get("CM_PORT")
  lazy val CMDatabase = get("CM_DATABASE")
  lazy val CMUser = get("CM_USER")
  lazy val CMPassword = get("CM_PASSWORD")

  val MappingHost = "mapping-api"
  val IsoMappingCacheAgeInMs = 1000 * 60 * 60 // 1 hour caching
  val TopicAPIUrl = "http://api.topic.ndla.no/rest/v1/keywords/?filter[node]=ndlanode_"

  lazy val MigrationHost = get("MIGRATION_HOST")
  lazy val MigrationUser = get("MIGRATION_USER")
  lazy val MigrationPassword = get("MIGRATION_PASSWORD")

  val resourceHtmlEmbedTag = "embed"

  // MathML element reference list: https://developer.mozilla.org/en/docs/Web/MathML/Element
  val mathJaxTags = Set("math", "maction", "maligngroup", "malignmark", "menclose", "merror", "mfenced", "mfrac", "mglyph", "mi",
    "mlabeledtr", "mlongdiv", "mmultiscripts", "mn", "mo", "mover", "mpadded", "mphantom", "mroot", "mrow", "ms", "mscarries",
    "mscarry", "msgroup", "msline", "mspace", "msqrt", "msrow", "mstack", "mstyle", "msub", "msup", "msubsup", "mtable", "mtd",
    "mtext", "mtr", "munder", "munderover", "semantics", "annotation", "annotation-xml")
  val permittedHTMLTags = Set("body", "article", "section", "table", "tr", "td", "li", "a", "button", "div", "p", "pre", "code", "sup",
    "h1", "h2", "h3", "h4", "h5", "h6", "aside", "strong", "ul", "br", "ol", "i", "em", "b", "th", "tbody", "blockquote",
    "details", "summary", "table", "thead", "tfoot", "tbody", "caption", "audio", "figcaption", "figure", resourceHtmlEmbedTag) ++ mathJaxTags

  val permittedHTMLAttributes = Set("data-resource", "data-id", "data-content-id", "data-link-text", "data-url",
    "data-size", "data-videoid", "data-account", "data-player", "data-key", "data-alt", "data-caption", "data-align", "data-nrk-video-id", "href", "title")


  def verify() = {
    val missingProperties = ContentApiProps.filter(entry => entry._2.isEmpty).toList
    if(missingProperties.nonEmpty){
      missingProperties.foreach(entry => logger.error("Missing required environment variable {}", entry._1))

      logger.error("Shutting down.")
      System.exit(1)
    }
  }

  def setProperties(properties: Map[String, Option[String]]) = {
    properties.foreach(prop => ContentApiProps.put(prop._1, prop._2))
  }

  def get(envKey: String): String = {
    ContentApiProps.get(envKey).flatten match {
      case Some(value) => value
      case None => throw new NoSuchFieldError(s"Missing environment variable $envKey")
    }
  }

  def getInt(envKey: String):Integer = {
    get(envKey).toInt
  }

  private def getBoolean(envKey: String): Boolean = {
    get(envKey).toBoolean
  }
}

object PropertiesLoader {
  val EnvironmentFile = "/article-api.env"

  def readPropertyFile(): Map[String,Option[String]] = {
    val keys = Source.fromInputStream(getClass.getResourceAsStream(EnvironmentFile)).getLines().withFilter(line => line.matches("^\\w+$"))
    keys.map(key => key -> scala.util.Properties.envOrNone(key)).toMap
  }

  def load() = {
    ArticleApiProperties.setProperties(readPropertyFile())
    ArticleApiProperties.verify()
  }
}
