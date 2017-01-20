/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.converters

import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import no.ndla.articleapi.ArticleApiProperties.resourceHtmlEmbedTag

class HtmlTagGeneratorTest extends UnitSuite with TestEnvironment {
    val sampleDataAttributes = Map(
      Attributes.DataResource -> ResourceType.Image.toString,
      Attributes.DataUrl -> "http://localhost/1",
      Attributes.DataCaption -> "Sample image"
    )

  test("A correctly formatted figure tag is returned") {
    val figureString: String = HtmlTagGenerator.buildEmbedContent(sampleDataAttributes)
    val expected = s"""<$resourceHtmlEmbedTag data-caption="Sample image" data-id="1" data-resource="image" data-url="http://localhost/1" />"""

    figureString should equal(expected)
  }

}
