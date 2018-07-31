/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi

import org.scalatra.ScalatraServlet
import org.scalatra.swagger._

class ResourcesApp(implicit val swagger: Swagger) extends ScalatraServlet with NativeSwaggerBase {
  get("/") {
    renderSwagger2(swagger.docs.toList)
  }
}

object ArticleApiInfo {

  val apiInfo = ApiInfo(
    "Article API",
    "Services for accessing articles and concepts",
    "http://ndla.no",
    ArticleApiProperties.ContactEmail,
    "GPL v3.0",
    "http://www.gnu.org/licenses/gpl-3.0.en.html"
  )
}

class ArticleSwagger extends Swagger("2.0", "1.0", ArticleApiInfo.apiInfo) {

  private def writeRolesInTest: List[String] = {
    val writeRoles = List(ArticleApiProperties.DraftRoleWithWriteAccess, ArticleApiProperties.RoleWithWriteAccess)
    writeRoles.map(_.replace(":", "-test:"))
  }

  addAuthorization(
    OAuth(writeRolesInTest,
          List(ImplicitGrant(LoginEndpoint(ArticleApiProperties.Auth0LoginEndpoint), "access_token"))))

}
