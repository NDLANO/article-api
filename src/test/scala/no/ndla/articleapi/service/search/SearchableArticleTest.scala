/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */
package no.ndla.articleapi.service.search

import no.ndla.articleapi.model.search.LanguageValue
import no.ndla.articleapi.{TestEnvironment, UnitSuite}

class SearchableArticleTest extends UnitSuite with TestEnvironment {

  test("Language with empty Some should convert language to None") {
    val lv = LanguageValue(Some(""), "This is ikke bare una Sprache")
    lv.lang should equal(None)
  }

}
