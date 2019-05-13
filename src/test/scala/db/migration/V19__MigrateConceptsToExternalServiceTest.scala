/*
 * Part of NDLA article-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{spy, _}

import scala.util.{Failure, Success}

class V19__MigrateConceptsToExternalServiceTest extends UnitSuite with TestEnvironment {
  val migration = spy(new V19__MigrateConceptsToExternalService)

  val content =
    """<section> <embed data-align=\"\" data-alt=\"Alt text for image\" data-caption=\"Image caption\" data-resource=\"image\" data-resource_id=\"4765\" data-size=\"full\"> <h2>Test</h2> <p>This is paragraph</p> <h3>Header of sorts</h3> <p>Text before concept <embed data-content-id=\"5\" data-link-text=\"filtrat\" data-resource=\"concept\"> Text after concept (<em></em><span lang=\"en\"><em>english</em></span>) Now comes another concept <embed data-content-id=\"1\" data-link-text=\"permeabilitet\" data-resource=\"concept\">.</p> <p>After paragraph</p></section>"""

  val json =
    s"""{"content":[{"content":"$content","language":"nb"}],"title":[{"title":"123 No change pls","language":"nb"}]}"""

  test("That concept ids are replaced with new ids in content") {
    val concept1 = migration.FetchedConcept(10, Some("5"), migration.LanguageInfo("Bokmål", "nb"))
    val concept2 = migration.FetchedConcept(2, Some("1"), migration.LanguageInfo("Bokmål", "nb"))
    when(migration.getExplanationIdFromConceptId("5")).thenReturn(Success(List(concept1)))
    when(migration.getExplanationIdFromConceptId("1")).thenReturn(Success(List(concept2)))

    val expectedContent = json
      .replace("""data-content-id=\"1\"""", """data-content-id=\"2\"""")
      .replace("""data-content-id=\"5\"""", """data-content-id=\"10\"""")

    migration.convertArticle(json) should be(expectedContent)
  }

  test("That concept ids are left as is if something went wrong") {
    when(migration.getExplanationIdFromConceptId(any[String]))
      .thenReturn(Failure(new RuntimeException("Something went wrong when fetching...")))
    migration.convertArticle(json) should be(json)

    when(migration.getExplanationIdFromConceptId(any[String])).thenReturn(Success(List.empty))
    migration.convertArticle(json) should be(json)
  }

  test("That concepts are found in correct language") {
    val expectedEnContent = content
      .replace("""data-content-id=\"1\"""", """data-content-id=\"100\"""")
      .replace("""data-content-id=\"5\"""", """data-content-id=\"200\"""")

    val expectedNbContent = content
      .replace("""data-content-id=\"1\"""", """data-content-id=\"300\"""")
      .replace("""data-content-id=\"5\"""", """data-content-id=\"400\"""")

    val multiLangJson =
      s"""{"content":[{"content":"$content","language":"nb"},{"content":"$content","language":"en"}],"title":[{"title":"123 No change pls","language":"nb"}]}"""
    val expectedMultiLangJson =
      s"""{"content":[{"content":"$expectedNbContent","language":"nb"},{"content":"$expectedEnContent","language":"en"}],"title":[{"title":"123 No change pls","language":"nb"}]}"""

    val enConcept1 = migration.FetchedConcept(100, Some("1"), migration.LanguageInfo("English", "en"))
    val enConcept2 = migration.FetchedConcept(200, Some("5"), migration.LanguageInfo("English", "en"))

    val nbConcept1 = migration.FetchedConcept(300, Some("1"), migration.LanguageInfo("Bokmål", "nb"))
    val nbConcept2 = migration.FetchedConcept(400, Some("5"), migration.LanguageInfo("Bokmål", "nb"))

    when(migration.getExplanationIdFromConceptId("1")).thenReturn(Success(List(enConcept1, nbConcept1)))
    when(migration.getExplanationIdFromConceptId("5")).thenReturn(Success(List(enConcept2, nbConcept2)))

    migration.convertArticle(multiLangJson) should be(expectedMultiLangJson)
  }

  test("That concepts are selected in prioritized language if not found in correct language") {
    val enJson =
      s"""{"content":[{"content":"$content","language":"en"}],"title":[{"title":"123 No change pls","language":"nb"}]}"""
    val enConcept1 = migration.FetchedConcept(10, Some("1"), migration.LanguageInfo("English", "en"))
    val nbConcept1 = migration.FetchedConcept(11, Some("1"), migration.LanguageInfo("Bokmål", "nb"))
    val nnConcept1 = migration.FetchedConcept(12, Some("1"), migration.LanguageInfo("Nynorsk", "nn"))

    val nbConcept2 = migration.FetchedConcept(20, Some("5"), migration.LanguageInfo("Bokmål", "nb"))
    val nnConcept2 = migration.FetchedConcept(21, Some("5"), migration.LanguageInfo("Nynorsk", "nn"))
    val zhConcept2 = migration.FetchedConcept(22, Some("5"), migration.LanguageInfo("Chinese", "zh"))

    when(migration.getExplanationIdFromConceptId("1")).thenReturn(Success(List(enConcept1, nbConcept1, nnConcept1)))
    when(migration.getExplanationIdFromConceptId("5")).thenReturn(Success(List(nbConcept2, nnConcept2)))

    val expectedContent1 = content
      .replace("""data-content-id=\"1\"""", """data-content-id=\"10\"""")
      .replace("""data-content-id=\"5\"""", """data-content-id=\"20\"""")
    val expectedJson1 =
      s"""{"content":[{"content":"$expectedContent1","language":"en"}],"title":[{"title":"123 No change pls","language":"nb"}]}"""
    migration.convertArticle(enJson) should be(expectedJson1)

    when(migration.getExplanationIdFromConceptId("5")).thenReturn(Success(List(nnConcept2, zhConcept2)))

    val expectedContent2 = content
      .replace("""data-content-id=\"1\"""", """data-content-id=\"10\"""")
      .replace("""data-content-id=\"5\"""", """data-content-id=\"21\"""")
    val expectedJson2 =
      s"""{"content":[{"content":"$expectedContent2","language":"en"}],"title":[{"title":"123 No change pls","language":"nb"}]}"""

    migration.convertArticle(enJson) should be(expectedJson2)
  }

}
