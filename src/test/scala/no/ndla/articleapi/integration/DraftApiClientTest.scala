/*
 * Part of NDLA article-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.integration

import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import no.ndla.articleapi.model.api
import org.json4s.DefaultFormats
import org.json4s.native.Serialization.write

class DraftApiClientTest extends UnitSuite with TestEnvironment {
  implicit val formats: DefaultFormats = DefaultFormats
  override val ndlaClient = new NdlaClient

  // Pact CDC imports
  import com.itv.scalapact.ScalaPactForger._
  import com.itv.scalapact.circe09._
  import com.itv.scalapact.http4s18._

  test("should be able to fetch agreements' copyright") {
    val expectedCopyright = api.Copyright(
      api.License("CC-BY-SA-4.0",
                  Some("Creative Commons Attribution-ShareAlike 4.0 International"),
                  Some("https://creativecommons.org/licenses/by-sa/4.0/")),
      "Origin",
      Seq.empty,
      Seq.empty,
      Seq.empty,
      None,
      None,
      None
    )
    val body = write(Agreement(1, expectedCopyright))

    forgePact
      .between("article-api")
      .and("draft-api")
      .addInteraction(
        interaction
          .description("Fetching agreement with id 1")
          .given("agreements")
          .uponReceiving("/draft-api/v1/agreements/1")
          .willRespondWith(
            status = 200,
            body = body
          )
      )
      .runConsumerTest { mockConfig =>
        val draftApiClient = new DraftApiClient(mockConfig.baseUrl)
        val copyright = draftApiClient.getAgreementCopyright(1)
        copyright.get should be(expectedCopyright)
      }
  }

}
