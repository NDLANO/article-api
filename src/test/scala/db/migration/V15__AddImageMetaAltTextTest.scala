/*
 * Part of NDLA article-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package db.migration
import no.ndla.articleapi.{TestEnvironment, UnitSuite}

class V15__AddImageMetaAltTextTest extends UnitSuite with TestEnvironment {
  val migration = new V15__AddImageMetaAltText

  test("migration should add empty alttext if no alttext exists") {
    val before = """{"metaImage":[{"imageId":"123","language":"nb"}],"title":[{"title":"tittel","language":"nb"}]}"""
    val expected =
      """{"metaImage":[{"imageId":"123","altText":"","language":"nb"}],"title":[{"title":"tittel","language":"nb"}]}"""

    migration.convertArticleUpdate(before) should equal(expected)
  }

  test("migration not do anyhting if the document already has alttext") {
    val original =
      """{"metaImage":[{"imageId":"123","altText":"du er en kreps","language":"nb"}],"title":[{"title":"tittel","language":"nb"}]}"""

    migration.convertArticleUpdate(original) should equal(original)
  }

}
