/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.controller

import no.ndla.articleapi.model.api._
import no.ndla.articleapi.model.domain.{ArticleType, Language, SearchResult, Sort}
import no.ndla.articleapi.{ArticleSwagger, TestData, TestEnvironment, UnitSuite}
import org.scalatra.test.scalatest.ScalatraFunSuite
import org.mockito.Mockito._
import org.mockito.Matchers._
import no.ndla.mapping.License.getLicenses
import org.json4s.native.Serialization.read

import scala.util.Success

class ArticleControllerV2Test extends UnitSuite with TestEnvironment with ScalatraFunSuite {

  val jwtHeader = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9"

  val jwtClaims = "eyJhcHBfbWV0YWRhdGEiOnsicm9sZXMiOlsiYXJ0aWNsZXM6d3JpdGUiXSwibmRsYV9pZCI6ImFiYzEyMyJ9LCJuYW1lIjoiRG9uYWxkIER1Y2siLCJpc3MiOiJodHRwczovL3NvbWUtZG9tYWluLyIsInN1YiI6Imdvb2dsZS1vYXV0aDJ8MTIzIiwiYXVkIjoiYWJjIiwiZXhwIjoxNDg2MDcwMDYzLCJpYXQiOjE0ODYwMzQwNjN9"
  val jwtClaimsNoRoles = "eyJhcHBfbWV0YWRhdGEiOnsicm9sZXMiOltdLCJuZGxhX2lkIjoiYWJjMTIzIn0sIm5hbWUiOiJEb25hbGQgRHVjayIsImlzcyI6Imh0dHBzOi8vc29tZS1kb21haW4vIiwic3ViIjoiZ29vZ2xlLW9hdXRoMnwxMjMiLCJhdWQiOiJhYmMiLCJleHAiOjE0ODYwNzAwNjMsImlhdCI6MTQ4NjAzNDA2M30"
  val jwtClaimsWrongRole = "eyJhcHBfbWV0YWRhdGEiOnsicm9sZXMiOlsic29tZTpvdGhlciJdLCJuZGxhX2lkIjoiYWJjMTIzIn0sIm5hbWUiOiJEb25hbGQgRHVjayIsImlzcyI6Imh0dHBzOi8vc29tZS1kb21haW4vIiwic3ViIjoiZ29vZ2xlLW9hdXRoMnwxMjMiLCJhdWQiOiJhYmMiLCJleHAiOjE0ODYwNzAwNjMsImlhdCI6MTQ4NjAzNDA2M30"

  val authHeaderWithWriteRole = s"Bearer $jwtHeader.$jwtClaims.VxqM2bu2UF8IAalibIgdRdmsTDDWKEYpKzHPbCJcFzA"
  val authHeaderWithoutAnyRoles = s"Bearer $jwtHeader.$jwtClaimsNoRoles.kXjaQ9QudcRHTqhfrzKr0Zr4pYISBfJoXWHVBreDyO8"
  val authHeaderWithWrongRole = s"Bearer $jwtHeader.$jwtClaimsWrongRole.JsxMW8y0hCmpuu9tpQr6ZdfcqkOS8hRatFi3cTO_PvY"

  implicit val formats = org.json4s.DefaultFormats
  implicit val swagger = new ArticleSwagger

  lazy val controller = new ArticleControllerV2
  addServlet(controller, "/test")

  val updateTitleJson = """{"revision": 1, "title": "hehe", "language": "nb", "content": "content"}"""
  val invalidArticle = """{"revision": 1, "title": [{"language": "nb", "titlee": "lol"]}"""
  val lang = "nb"
  val articleId = 1

  test("/<article_id> should return 200 if the cover was found withIdV2") {
    when(readService.withIdV2(articleId, lang)).thenReturn(Some(TestData.sampleArticleV2))

    get(s"/test/$articleId?language=$lang") {
      status should equal(200)
    }
  }

  test("/<article_id> should return 404 if the article was not found withIdV2") {
    when(readService.withIdV2(articleId, lang)).thenReturn(None)

    get(s"/test/$articleId?language=$lang") {
      status should equal(404)
    }
  }

  test("/<article_id> should return 400 if the article was not found withIdV2") {
    get(s"/test/one") {
      status should equal(400)
    }
  }


  test("POST / should return 400 on failure to validate request") {
    post("/test/", "{}", headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal(400)
    }
  }

  test("POST / should return 201 on created") {
    when(writeService.newArticleV2(any[NewArticleV2])).thenReturn(Success(TestData.sampleArticleV2))
    post("/test/", TestData.requestNewArticleV2Body, headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal(201)
    }
  }

  test("That / returns a validation message if article is invalid") {
    post("/test", headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal (400)
    }
  }

  test("That POST / returns 403 if no auth-header") {
    post("/test") {
      status should equal (403)
    }
  }

  test("That POST / returns 403 if auth header does not have expected role") {
    post("/test", headers = Map("Authorization" -> authHeaderWithWrongRole)) {
      status should equal (403)
    }
  }

  test("That POST / returns 403 if auth header does not have any roles") {
    post("/test", headers = Map("Authorization" -> authHeaderWithoutAnyRoles)) {
      status should equal (403)
    }
  }

  test("That PATCH /:id returns a validation message if article is invalid") {
    patch("/test/123", invalidArticle, headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal (400)
    }
  }

  test("That PATCH /:id returns 403 if no auth-header") {
    patch("/test/123") {
      status should equal (403)
    }
  }

  test("That PATCH /:id returns 403 if auth header does not have expected role") {
    patch("/test/123", headers = Map("Authorization" -> authHeaderWithWrongRole)) {
      status should equal (403)
    }
  }

  test("That PATCH /:id returns 403 if auth header does not have any roles") {
    patch("/test/123", headers = Map("Authorization" -> authHeaderWithoutAnyRoles)) {
      status should equal (403)
    }
  }

  test("That PATCH /:id returns 200 on success") {
    when(writeService.updateArticleV2(any[Long], any[UpdatedArticleV2])).thenReturn(Success(TestData.apiArticleWithHtmlFaultV2))
    patch("/test/123", updateTitleJson, headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal (200)
    }
  }

  test ("That GET /licenses with filter sat to by only returns creative common licenses") {
    val creativeCommonlicenses = getLicenses.filter(_.license.startsWith("by")).map(l => License(l.license, Option(l.description), l.url)).toSet

    get("/test/licenses", "filter" -> "by") {
      status should equal (200)
      val convertedBody = read[Set[License]](body)
      convertedBody should equal(creativeCommonlicenses)
    }
  }

  test ("That GET /licenses with filter not specified returns all licenses") {
    val allLicenses = getLicenses.map(l => License(l.license, Option(l.description), l.url)).toSet

    get("/test/licenses") {
      status should equal (200)
      val convertedBody = read[Set[License]](body)
      convertedBody should equal(allLicenses)
    }
  }

  test("GET / should use size of id-list as page-size if defined") {
    val searchMock = mock[SearchResult]
    val searchResultMock = mock[io.searchbox.core.SearchResult]
    when(articleSearchService.all(any[List[Long]], any[String], any[Option[String]], any[Int], any[Int], any[Sort.Value], any[Seq[String]]))
      .thenReturn(searchMock)
    when(searchMock.response).thenReturn(searchResultMock)
    when(converterService.getHitsV2(searchResultMock, "nb")).thenReturn(Seq.empty)

    get("/test/", "ids" -> "1,2,3,4", "page-size" -> "10", "language" -> "nb") {
      status should equal (200)
      verify(articleSearchService, times(1)).all(List(1, 2, 3, 4), Language.DefaultLanguage, None, 1, 4, Sort.ByTitleAsc, ArticleType.all)
    }
  }
}
