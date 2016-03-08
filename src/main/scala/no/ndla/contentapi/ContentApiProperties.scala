/*
 * Part of NDLA Content-API. API for searching and downloading content from NDLA.
 * Copyright (C) 2015 NDLA
 *
 * See LICENSE
 */
package no.ndla.contentapi

import com.typesafe.scalalogging.LazyLogging

object ContentApiProperties extends LazyLogging {


  val ApplicationPort = 80
  val EnvironmentFile = "/content-api.env"
  val ContentApiProps = readPropertyFile()

  val ContactEmail = get("CONTACT_EMAIL")
  val HostAddr = get("HOST_ADDR")
  val Domains = get("DOMAINS").split(",") ++ Array(HostAddr)

  val SearchHost = "search-engine"
  val SearchPort = get("SEARCH_ENGINE_ENV_TCP_PORT")
  var SearchClusterName = get("SEARCH_ENGINE_ENV_CLUSTER_NAME")
  val SearchIndex = get("SEARCH_INDEX")
  val SearchDocument = get("SEARCH_DOCUMENT")
  val DefaultPageSize: Int = getInt("SEARCH_DEFAULT_PAGE_SIZE")
  val MaxPageSize: Int = getInt("SEARCH_MAX_PAGE_SIZE")
  val IndexBulkSize = getInt("INDEX_BULK_SIZE")

  def verify() = {
    val missingProperties = ContentApiProps.filter(entry => entry._2.isEmpty).toList
    if(missingProperties.length > 0){
      missingProperties.foreach(entry => logger.error("Missing required environment variable {}", entry._1))

      logger.error("Shutting down.")
      System.exit(1)
    }
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

  def readPropertyFile(): Map[String,Option[String]] = {
    val keys = io.Source.fromInputStream(getClass.getResourceAsStream(EnvironmentFile)).getLines().withFilter(line => line.matches("^\\w+$"))
    keys.map(key => key -> scala.util.Properties.envOrNone(key)).toMap
  }

}
