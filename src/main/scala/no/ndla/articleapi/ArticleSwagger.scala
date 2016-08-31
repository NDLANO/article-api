/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi

import org.scalatra.ScalatraServlet
import org.scalatra.swagger.{ApiInfo, NativeSwaggerBase, Swagger}

class ResourcesApp(implicit val swagger: Swagger) extends ScalatraServlet with NativeSwaggerBase

object ArticleApiInfo {
  val apiInfo = ApiInfo(
    "Article API",
    "Documentation for the Article API of NDLA.no",
    "http://ndla.no",
    ContentApiProperties.ContactEmail,
    "GPL v3.0",
    "http://www.gnu.org/licenses/gpl-3.0.en.html")
}

class ArticleSwagger extends Swagger(Swagger.SpecVersion, "0.8", ArticleApiInfo.apiInfo)
