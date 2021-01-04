package db.migration

import no.ndla.articleapi.{TestEnvironment, UnitSuite}

class V27__AddConceptIdsToArticleTest extends UnitSuite with TestEnvironment {
  val migration = new V27__AddConceptIdsToArticle

  test("conceptIds should be added to article content") {
    val old =
      s"""{"metaDescription":[{"content":"what","language":"nb"}],"articleType":"standard","status":{"current":"PUBLISHED","other":[]},"updated":"2018-01-01T13:00:00Z","published":"2018-01-01T13:00:00Z"}"""
    val expected =
      s"""{"metaDescription":[{"content":"what","language":"nb"}],"articleType":"standard","status":{"current":"PUBLISHED","other":[]},"updated":"2018-01-01T13:00:00Z","published":"2018-01-01T13:00:00Z","conceptIds":[]}"""

    migration.convertArticleUpdate(old) should be(expected)
  }
}
