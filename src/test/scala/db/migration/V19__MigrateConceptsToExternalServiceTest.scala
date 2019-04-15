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

class V19__MigrateConceptsToExternalServiceTest extends UnitSuite with TestEnvironment {
  val migration = spy(new V19__MigrateConceptsToExternalService)

  val content =
    """<section> <embed data-align=\"\" data-alt=\"Alt text for image\" data-caption=\"Image caption\" data-resource=\"image\" data-resource_id=\"4765\" data-size=\"full\"> <h2>Test</h2> <p>This is paragraph</p> <h3>Header of sorts</h3> <p>Text before concept <embed data-content-id=\"5\" data-link-text=\"filtrat\" data-resource=\"concept\"> Text after concept (<em></em><span lang=\"en\"><em>english</em></span>) Now comes another concept <embed data-content-id=\"1\" data-link-text=\"permeabilitet\" data-resource=\"concept\">.</p> <p>After paragraph</p></section>"""

  val json =
    s"""{"content":[{"content":"$content","language":"nb"}],"title":[{"title":"123 No change pls","language":"nb"}]}"""

  test("That concept ids are replaced with new ids in content") {
    when(migration.getExplanationIdFromConceptId("5")).thenReturn(Some(10))
    when(migration.getExplanationIdFromConceptId("1")).thenReturn(Some(2))

    val expectedContent = json
      .replace("""data-content-id=\"1\"""", """data-content-id=\"2\"""")
      .replace("""data-content-id=\"5\"""", """data-content-id=\"10\"""")

    migration.convertArticle(json) should be(expectedContent)
  }

  test("That concept ids are left as is if something went wrong") {
    when(migration.getExplanationIdFromConceptId(any[String])).thenReturn(None)
    migration.convertArticle(json) should be(json)
  }

}
