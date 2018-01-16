/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.controller

import no.ndla.articleapi.model.api.{NewConcept, UpdatedConcept}
import no.ndla.articleapi.{ArticleSwagger, TestData, TestEnvironment, UnitSuite}
import org.json4s.native.Serialization.write
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatra.test.scalatest.ScalatraFunSuite

import scala.util.Success

class ConceptControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {
  implicit val formats = org.json4s.DefaultFormats
  implicit val swagger = new ArticleSwagger

  val authHeaderWithWriteRole = s"Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhcHBfbWV0YWRhdGEiOnsicm9sZXMiOlsiYXJ0aWNsZXM6d3JpdGUiXSwibmRsYV9pZCI6ImFiYzEyMyJ9LCJuYW1lIjoiRG9uYWxkIER1Y2siLCJpc3MiOiJodHRwczovL3NvbWUtZG9tYWluLyIsInN1YiI6Imdvb2dsZS1vYXV0aDJ8MTIzIiwiYXVkIjoiYWJjIiwiZXhwIjoxNDg2MDcwMDYzLCJpYXQiOjE0ODYwMzQwNjN9.VxqM2bu2UF8IAalibIgdRdmsTDDWKEYpKzHPbCJcFzA"

  lazy val controller = new ConceptController
  addServlet(controller, "/test")

  val conceptId = 1
  val lang = "nb"

  test("/<concept_id> should return 200 if the cover was found") {
    when(readService.conceptWithId(1, lang)).thenReturn(Some(TestData.sampleApiConcept))
    get(s"/test/$conceptId?language=$lang") {
      status should equal(200)
    }
  }

  test("/<concept_id> should return 404 if the article was not found") {
    when(readService.conceptWithId(conceptId, lang)).thenReturn(None)

    get(s"/test/$conceptId?language=$lang") {
      status should equal(404)
    }
  }

  test("/<concept_id> should return 400 if the article was not found") {
    get(s"/test/one") {
      status should equal(400)
    }
  }

}
