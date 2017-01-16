/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.controller

import no.ndla.articleapi.{ArticleSwagger, TestEnvironment, UnitSuite}
import org.scalatra.test.scalatest.ScalatraFunSuite

class ArticleControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {
  implicit val formats = org.json4s.DefaultFormats
  implicit val swagger = new ArticleSwagger

  lazy val controller = new ArticleController
  addServlet(controller, "/")

  test("That / returns a validation message if article is invalid") {
    post("/") {
      status should equal (400)
    }
  }

}
