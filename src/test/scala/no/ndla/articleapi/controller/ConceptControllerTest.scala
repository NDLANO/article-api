/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.controller

import no.ndla.articleapi.model.api._
import no.ndla.articleapi.model.domain.Sort
import no.ndla.articleapi.model.search.SearchResult
import no.ndla.articleapi.{ArticleSwagger, TestData, TestEnvironment, UnitSuite}
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatra.test.scalatest.ScalatraFunSuite

import scala.util.{Failure, Success}

class ConceptControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {
  implicit val formats = org.json4s.DefaultFormats
  implicit val swagger = new ArticleSwagger

  val authHeaderWithWriteRole =
    s"Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhcHBfbWV0YWRhdGEiOnsicm9sZXMiOlsiYXJ0aWNsZXM6d3JpdGUiXSwibmRsYV9pZCI6ImFiYzEyMyJ9LCJuYW1lIjoiRG9uYWxkIER1Y2siLCJpc3MiOiJodHRwczovL3NvbWUtZG9tYWluLyIsInN1YiI6Imdvb2dsZS1vYXV0aDJ8MTIzIiwiYXVkIjoiYWJjIiwiZXhwIjoxNDg2MDcwMDYzLCJpYXQiOjE0ODYwMzQwNjN9.VxqM2bu2UF8IAalibIgdRdmsTDDWKEYpKzHPbCJcFzA"

  lazy val controller = new ConceptController
  addServlet(controller, "/test")

  val conceptId = 1
  val lang = "nb"

  test("/<concept_id> should return 200 if the cover was found") {
    when(readService.conceptWithId(1, lang, fallback = false)).thenReturn(Success(TestData.sampleApiConcept))
    get(s"/test/$conceptId?language=$lang") {
      status should equal(200)
    }
  }

  test("/<concept_id> should return 404 if the article was not found") {
    when(readService.conceptWithId(conceptId, lang, fallback = false)).thenReturn(Failure(NotFoundException("nope")))

    get(s"/test/$conceptId?language=$lang") {
      status should equal(404)
    }
  }

  test("/<concept_id> should return 400 if the article was not found") {
    get(s"/test/one") {
      status should equal(400)
    }
  }

  test("That scrollId is in header, and not in body") {
    val scrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="
    val searchResponse = SearchResult[ConceptSummary](
      0,
      Some(1),
      10,
      "nb",
      Seq.empty[ConceptSummary],
      Some(scrollId)
    )
    when(
      conceptSearchService
        .all(any[List[Long]], any[String], any[Int], any[Int], any[Sort.Value], any[Boolean]))
      .thenReturn(Success(searchResponse))
    get(s"/test/") {
      status should be(200)
      body.contains(scrollId) should be(false)
      header("search-context") should be(scrollId)
    }
  }

  test("That scrolling uses scroll and not searches normally") {
    reset(conceptSearchService)
    val scrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="
    val searchResponse = SearchResult[ConceptSummary](
      0,
      Some(1),
      10,
      "nb",
      Seq.empty[ConceptSummary],
      Some(scrollId)
    )

    when(conceptSearchService.scroll(anyString, anyString, anyBoolean)).thenReturn(Success(searchResponse))

    get(s"/test?search-context=$scrollId") {
      status should be(200)
    }

    verify(conceptSearchService, times(0)).all(any[List[Long]],
                                               any[String],
                                               any[Int],
                                               any[Int],
                                               any[Sort.Value],
                                               any[Boolean])
    verify(conceptSearchService, times(0)).matchingQuery(any[String],
                                                         any[List[Long]],
                                                         any[String],
                                                         any[Int],
                                                         any[Int],
                                                         any[Sort.Value],
                                                         any[Boolean])
    verify(conceptSearchService, times(1)).scroll(eqTo(scrollId), any[String], any[Boolean])
  }

  test("That scrolling with POST uses scroll and not searches normally") {
    reset(conceptSearchService)
    val scrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="
    val searchResponse = SearchResult[ConceptSummary](
      0,
      Some(1),
      10,
      "nb",
      Seq.empty[ConceptSummary],
      Some(scrollId)
    )

    when(conceptSearchService.scroll(anyString, anyString, anyBoolean)).thenReturn(Success(searchResponse))

    post(s"/test/search/?search-context=$scrollId") {
      status should be(200)
    }

    verify(conceptSearchService, times(0)).all(any[List[Long]],
                                               any[String],
                                               any[Int],
                                               any[Int],
                                               any[Sort.Value],
                                               any[Boolean])
    verify(conceptSearchService, times(0)).matchingQuery(any[String],
                                                         any[List[Long]],
                                                         any[String],
                                                         any[Int],
                                                         any[Int],
                                                         any[Sort.Value],
                                                         any[Boolean])
    verify(conceptSearchService, times(1)).scroll(eqTo(scrollId), any[String], any[Boolean])
  }

}
