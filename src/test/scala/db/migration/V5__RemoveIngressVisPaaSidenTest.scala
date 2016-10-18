/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package db.migration

import no.ndla.articleapi.{TestEnvironment, UnitSuite}

class V5__RemoveIngressVisPaaSidenTest extends UnitSuite with TestEnvironment {
  val migrator = new V5__RemoveIngressVisPaaSiden

  test("That converting old document converts to expected new document") {
    val oldDocument =
      """{"id":"0","introduction":[{"introduction":"<p>statistiske data</p>","image":"5452","displayIngress":false,"language":"nb"},
        |{"introduction":"<p>stutustuske dutu</p>","image":"5452","displayIngress":false,"language":"nn"}]}""".stripMargin.replace("\n", "")
    val newDocument =
      """{"id":"0","introduction":[{"introduction":"<p>statistiske data</p>","image":"5452","language":"nb"},
                                  |{"introduction":"<p>stutustuske dutu</p>","image":"5452","language":"nn"}]}""".stripMargin.replace("\n", "")

    migrator.convertDocumentToNewFormat(V5_DBContent(1, oldDocument)).document should equal (newDocument)
  }

}
