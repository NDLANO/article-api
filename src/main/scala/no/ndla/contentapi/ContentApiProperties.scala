/*
 * Part of NDLA Content-API. API for searching and downloading content from NDLA.
 * Copyright (C) 2015 NDLA
 *
 * See LICENSE
 */
package no.ndla.contentapi

import com.typesafe.scalalogging.LazyLogging

object ContentApiProperties extends LazyLogging {

  val EnvironmentFile = "/content-api.env"
  val ContentApiProps = io.Source.fromInputStream(getClass.getResourceAsStream(EnvironmentFile)).getLines().map(key => key -> scala.util.Properties.envOrNone(key)).toMap

  val ContactEmail = get("CONTACT_EMAIL")
  val ApplicationPort = getInt("APPLICATION_PORT")
  val Domains = get("DOMAINS").split(",")

  def verify() = {
    val missingProperties = ContentApiProps.filter(entry => entry._2.isEmpty).toList
    if(missingProperties.length > 0){
      missingProperties.foreach(entry => logger.error("Missing required environment variable {}", entry._1))

      logger.error("Shutting down.")
      System.exit(1)
    }
  }

  def get(envKey: String): String = {
    ContentApiProps.get(envKey) match {
      case Some(value) => value.get
      case None => throw new NoSuchFieldError(s"Missing environment variable $envKey")
    }
  }

  def getInt(envKey: String):Integer = {
    get(envKey).toInt
  }

}
