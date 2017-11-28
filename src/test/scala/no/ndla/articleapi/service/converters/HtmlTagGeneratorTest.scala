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
import no.ndla.validation.{TagAttributes, ResourceType}

class HtmlTagGeneratorTest extends UnitSuite with TestEnvironment {
    val sampleDataAttributes = Map(
      TagAttributes.DataResource -> ResourceType.Image.toString,
      TagAttributes.DataUrl -> "http://localhost/1",
      TagAttributes.DataCaption -> "Sample image"
    )

    val sampleContentLink = Map(
      TagAttributes.DataResource -> ResourceType.ContentLink.toString,
      TagAttributes.DataContentId -> "2",
      TagAttributes.DataLinkText -> "http://localhost/2",
      TagAttributes.DataOpenIn -> "new-context"
    )

  test("A correctly formatted figure tag is returned") {
    val figureString: String = HtmlTagGenerator.buildEmbedContent(sampleDataAttributes)
    val expected = s"""<$ResourceHtmlEmbedTag data-caption="Sample image" data-resource="image" data-url="http://localhost/1" />"""

    figureString should equal(expected)
  }

  test("Correctly formatted content-link embed") {
    val contentLinkString: String = HtmlTagGenerator.buildEmbedContent(sampleContentLink)
    val expected = s"""<$ResourceHtmlEmbedTag data-content-id="2" data-link-text="http://localhost/2" data-open-in="new-context" data-resource="content-link" />"""

    contentLinkString should equal(expected)
  }

}
