/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.controller

import no.ndla.articleapi.{ArticleSwagger, TestData, TestEnvironment, UnitSuite}
import org.mockito.Mockito._
import org.scalatra.test.scalatest.ScalatraFunSuite

class ConceptControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {
  implicit val formats = org.json4s.DefaultFormats
  implicit val swagger = new ArticleSwagger

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
