/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.integration

import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import org.joda.time.DateTime

class MigrationApiClientTest extends UnitSuite with TestEnvironment {
  val migrationIngress = MigrationIngress("123", Option("ingress from  table"), None, 1, Option("nb"))
  val migrationContent= MigrationContent("124", "124", Some("content"), "metadescription", Option("nb"), DateTime.now().toDate, DateTime.now().toDate)
  val emneArtikkelData = MigrationEmneArtikkelData("ingress from emneartikkel", "metadescription from emneartikkel", Option("nb"))
  val migrationMainNodeImport = MigrationMainNodeImport(Seq(), Seq(migrationIngress), Seq(migrationContent), Seq(), Option("by-sa"),
    Option("emneartikkel"), Seq(), Seq(), Seq(), Seq(), Seq(), Seq(), Seq(), Seq(), Seq(), Seq(emneArtikkelData))

  test("asLanguageContents uses ingress from emneartikkel if emneartikkeldata is present") {
    migrationMainNodeImport.asLanguageContents.head.ingress.get should equal (LanguageIngress(emneArtikkelData.ingress, None))
  }

  test("asLanguageContents uses ingress from separate ingress field if present") {
    migrationMainNodeImport.copy(emneartikkelData=Seq()).asLanguageContents.head.ingress.get should equal (LanguageIngress(migrationIngress.content.get, migrationIngress.imageNid))
  }

  test("asLanguageContents uses metadescription from emneartikkel if present") {
    migrationMainNodeImport.asLanguageContents.head.metaDescription should equal (emneArtikkelData.metaDescription)
  }

  test("asLanguageContents uses metadescription from content if emneartikkel is not present") {
    migrationMainNodeImport.asLanguageContents.head.metaDescription should equal (emneArtikkelData.metaDescription)
  }
}
