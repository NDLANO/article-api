/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package db.migration

import no.ndla.articleapi.{TestEnvironment, UnitSuite}

class V2__ChangeTagStructureTest extends UnitSuite with TestEnvironment {
  val migrator = new V2__ChangeTagStructure()

  test("That convertTagsToNewFormat with no tags returns empty taglist") {
    val content = V2_DBContent (1, """{"tags":[]}""")
    val optConverted = migrator.convertTagsToNewFormat(content)

    optConverted.isDefined should be(true)
    optConverted.get.document should equal(content.document)
  }

  test("That converting an already converted content node returns none") {
    val content = V2_DBContent(2,"""{"tags":[{"tags": ["eple", "banan"], "language": "nb"}, {"tag": ["apple", "banana"], "language": "en"}]}""")
    migrator.convertTagsToNewFormat(content) should be(None)
  }

  test("That convertTagsToNewFormat converts to expected format") {
    val before = """{"tags": [{"tag": "eple", "language":"nb"}, {"tag": "banan", "language":"nb"}, {"tag": "apple", "language":"en"}, {"tag": "banana", "language":"en"}]}"""
    val expectedAfter = """{"tags":[{"tags":["eple","banan"],"language":"nb"},{"tags":["apple","banana"],"language":"en"}]}"""
    val content = V2_DBContent(3, before)

    val optConverted = migrator.convertTagsToNewFormat(content)
    optConverted.isDefined should be(true)
    optConverted.get.document should equal(expectedAfter)
  }
}
