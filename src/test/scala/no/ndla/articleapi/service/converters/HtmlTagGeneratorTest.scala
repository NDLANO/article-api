/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.converters

import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import no.ndla.validation.EmbedTagRules.ResourceHtmlEmbedTag
import no.ndla.validation.{Attributes, ResourceType}

class HtmlTagGeneratorTest extends UnitSuite with TestEnvironment {
    val sampleDataAttributes = Map(
      Attributes.DataResource -> ResourceType.Image.toString,
      Attributes.DataUrl -> "http://localhost/1",
      Attributes.DataCaption -> "Sample image"
    )

  test("A correctly formatted figure tag is returned") {
    val figureString: String = HtmlTagGenerator.buildEmbedContent(sampleDataAttributes)
    val expected = s"""<$ResourceHtmlEmbedTag data-caption="Sample image" data-resource="image" data-url="http://localhost/1" />"""

    figureString should equal(expected)
  }

}
