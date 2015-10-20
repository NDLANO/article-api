/*
 * Part of NDLA Content-API. API for searching and downloading content from NDLA.
 * Copyright (C) 2015 NDLA
 *
 * See LICENSE
 */
package no.ndla.contentapi

import org.scalatra.ScalatraServlet
import org.scalatra.swagger.{ApiInfo, NativeSwaggerBase, Swagger}

class ResourcesApp(implicit val swagger: Swagger) extends ScalatraServlet with NativeSwaggerBase

object ContentApiInfo {
  val apiInfo = ApiInfo(
    "Content Api",
    "Documentation for the Content API of NDLA.no",
    "http://ndla.no",
    ContentApiProperties.ContactEmail,
    "GPL v3.0",
    "http://www.gnu.org/licenses/gpl-3.0.en.html")
}

class ContentSwagger extends Swagger(Swagger.SpecVersion, "0.8", ContentApiInfo.apiInfo)
