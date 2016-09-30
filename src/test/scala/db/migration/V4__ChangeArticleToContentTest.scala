/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.articleapi.{TestEnvironment, UnitSuite}

class V4__ChangeArticleToContentTest extends UnitSuite with TestEnvironment {

  val migrator = new V4__ChangeArticleToContent

  test("That converting old document converts to expected new document") {
    val oldDocument = """{"id":"0","titles":[{"title":"Smittevern i helsetjenesten","language":"nb"}],"article":[{"article":"innhold","language":"nb"}]}"""
    val newDocument = """{"id":"0","title":[{"title":"Smittevern i helsetjenesten","language":"nb"}],"content":[{"content":"innhold","language":"nb"}]}"""

    migrator.convertDocumentToNewFormat(V4_DBContent(1, oldDocument)).document should equal (newDocument)
  }
}
