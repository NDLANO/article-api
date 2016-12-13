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

class HtmlFigureGeneratorTest extends UnitSuite with TestEnvironment {
    val sampleDataAttributes = Map(
      "resource" -> "image",
      "url" -> "http://localhost/1",
      "caption" -> "Sample image"
    )

  test("A correctly formatted figure tag is returned") {
    val (figureString, errorList) = HtmlTagGenerator.buildEmbedContent(sampleDataAttributes)

    figureString should equal(s"""<$resourceHtmlEmbedTag data-caption="Sample image" data-id="1" data-resource="image" data-url="http://localhost/1" />""")
    errorList.isEmpty should be (true)
  }

  test("An error message is returned if an illegal attribute is used") {
    val dataAttributes = sampleDataAttributes + ("illegal-attribute" -> "")
    val (figureString, errorList) = HtmlTagGenerator.buildEmbedContent(dataAttributes)

    figureString should equal(s"""<$resourceHtmlEmbedTag data-caption="Sample image" data-id="1" data-illegal-attribute="" data-resource="image" data-url="http://localhost/1" />""")
    errorList.isEmpty should be (false)
    errorList.head.contains("This is a BUG:") should be (true)
  }

}
