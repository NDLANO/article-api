/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.controller

import java.util.Date

import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.{TestData, TestEnvironment, UnitSuite}
import org.json4s.native.Serialization._
import org.scalatra.test.scalatest.ScalatraFunSuite
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._

import scala.util.{Failure, Success, Try}
import no.ndla.articleapi.TestData._
import no.ndla.articleapi.model.api.NewArticleV2
import scalikejdbc.DBSession

class InternControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {
  implicit val formats = org.json4s.DefaultFormats

  val author = Author("forfatter", "Henrik")

  val authHeaderWithWriteRole =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoieHh4eXl5IiwiaXNzIjoiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCJzdWIiOiJ4eHh5eXlAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNTEwMzA1NzczLCJleHAiOjE1MTAzOTIxNzMsInNjb3BlIjoiYXJ0aWNsZXM6d3JpdGUiLCJndHkiOiJjbGllbnQtY3JlZGVudGlhbHMifQ.kh82qM84FZgoo3odWbHTLWy-N049m7SyQw4gdatDMk43H2nWHA6gjsbJoiBIZ7BcbSfHElEZH0tP94vRy-kjgA3hflhOBbsD73DIxRvnbH1kSXlBnl6ISbgtHnzv1wQ7ShykMAcBsoWQ6J16ixK_p-msW42kcEqK1LanzPy-_qI"
  lazy val controller = new InternController
  addServlet(controller, "/*")

  override val authRole = new AuthRole

  test("POST /validate/article should return 400 if the article is invalid") {
    val invalidArticle = """{"revision": 1, "title": [{"language": "nb", "titlee": "lol"]}"""
    post("/validate/article", body = invalidArticle) {
      status should equal(400)
    }
  }

  test("POST /validate should return 204 if the article is valid") {
    when(contentValidator.validateArticle(any[Article], any[Boolean], any[Boolean]))
      .thenReturn(Success(TestData.sampleArticleWithByNcSa))
    post("/validate/article", body = write(TestData.sampleArticleWithByNcSa)) {
      status should equal(200)
    }
  }

}
