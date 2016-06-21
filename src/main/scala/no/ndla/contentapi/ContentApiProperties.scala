/*
 * Part of NDLA Content-API. API for searching and downloading content from NDLA.
 * Copyright (C) 2015 NDLA
 *
 * See LICENSE
 */
package no.ndla.contentapi

import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable

object ContentApiProperties extends LazyLogging {
  var ContentApiProps: mutable.Map[String, Option[String]] = mutable.HashMap()

  val ApplicationPort = 80

  // When converting a content node, the converter may run several times over the content to make sure
  // everything is converted. This value defines a maximum number of times the converter runs on a node
  val maxConvertionRounds = 5

  lazy val ContactEmail = get("CONTACT_EMAIL")
  lazy val HostAddr = get("HOST_ADDR")
  lazy val Domains = get("DOMAINS").split(",") ++ Array(HostAddr)

  lazy val imageApiBaseUrl = get("IMAGE_API_BASE_URL")
  val imageApiInternEndpointURLSuffix = "admin"
  val imageApiImportImageURL = s"$imageApiInternEndpointURLSuffix/import"
  val imageApiGetByExternalIdURL = s"$imageApiInternEndpointURLSuffix/extern"

  val ndlaBaseHost = "http://ndla.no/"

  val SearchHost = "search-engine"
  lazy val SearchPort = get("SEARCH_ENGINE_ENV_TCP_PORT")
  lazy val SearchClusterName = get("SEARCH_ENGINE_ENV_CLUSTER_NAME")
  lazy val SearchIndex = get("SEARCH_INDEX")
  lazy val SearchDocument = get("SEARCH_DOCUMENT")
  lazy val DefaultPageSize: Int = getInt("SEARCH_DEFAULT_PAGE_SIZE")
  lazy val MaxPageSize: Int = getInt("SEARCH_MAX_PAGE_SIZE")
  lazy val IndexBulkSize = getInt("INDEX_BULK_SIZE")

  lazy val CMHost = get("CM_HOST")
  lazy val CMPort = get("CM_PORT")
  lazy val CMDatabase = get("CM_DATABASE")
  lazy val CMUser = get("CM_USER")
  lazy val CMPassword = get("CM_PASSWORD")

  def verify() = {
    val missingProperties = ContentApiProps.filter(entry => entry._2.isEmpty).toList
    if(missingProperties.length > 0){
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
}

object PropertiesLoader {
  val EnvironmentFile = "/content-api.env"

  def readPropertyFile(): Map[String,Option[String]] = {
    val keys = io.Source.fromInputStream(getClass.getResourceAsStream(EnvironmentFile)).getLines().withFilter(line => line.matches("^\\w+$"))
    keys.map(key => key -> scala.util.Properties.envOrNone(key)).toMap
  }

  def load() = {
    ContentApiProperties.setProperties(readPropertyFile())
    ContentApiProperties.verify()
  }
}
