/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package db.migration

import no.ndla.articleapi.{TestEnvironment, UnitSuite}

class V7__RemoveIngressLicenseDescriptionAndUrl extends UnitSuite with TestEnvironment {
  val migrator = new V7__RemoveLicenseDescriptionAndUrl

  test("That converting old document converts to expected new document") {
    val oldDocument =
      """{"id":1,"copyright":{"origin":"","authors":[{"name":"Kari","type":"Forfatter"}],
        |"license":{"license":"by-sa","description":"","url":""}}}""".stripMargin.replace("\n", "")
    val newDocument =
      """{"id":1,"copyright":{"origin":"","authors":[{"name":"Kari","type":"Forfatter"}],
        |"license":"by-sa"}}""".stripMargin.replace("\n", "")

    migrator.convertDocumentToNewFormat(V7_DBContent(1, oldDocument)).document should equal (newDocument)
  }

  test("converting already converted document returns the same document") {
    val document =
      """{"id":1,"copyright":{"origin":"","authors":[{"name":"Kari","type":"Forfatter"}],
        |"license":"by-sa"}}""".stripMargin.replace("\n", "")

    migrator.convertDocumentToNewFormat(V7_DBContent(1, document)).document should equal (document)
  }

}
