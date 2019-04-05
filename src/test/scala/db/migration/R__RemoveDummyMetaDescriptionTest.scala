package db.migration
import no.ndla.articleapi.{TestEnvironment, UnitSuite}

/**
  * Part of NDLA ndla.
  * Copyright (C) 2019 NDLA
  *
  * See LICENSE
  */
class R__RemoveDummyMetaDescriptionTest extends UnitSuite with TestEnvironment {
  val migration = new R__RemoveDummyMetaDescription

  test("migration should remove Beskrivelse mangler from metadescription") {
    val before =
      """{"metaDescription":[{"content":"Beskrivelse mangler","language":"nb"},{"content":"Meta description","language":"nn"}],"title":[{"title":"tittel","language":"nb"},{"title":"tittel","language":"nn"}]}"""
    val expected =
      """{"metaDescription":[{"content":"","language":"nb"},{"content":"Meta description","language":"nn"}],"title":[{"title":"tittel","language":"nb"},{"title":"tittel","language":"nn"}]}"""

    migration.convertArticle(before) should equal(expected)
  }
}
