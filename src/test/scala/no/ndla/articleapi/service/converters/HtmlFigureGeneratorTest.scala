/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.converters

import no.ndla.articleapi.UnitSuite

class HtmlFigureGeneratorTest extends UnitSuite {
    val sampleDataAttributes = Map(
      "resource" -> "image",
      "id" -> "1",
      "url" -> "http://localhost/1",
      "caption" -> "Sample image"
    )

  test("A correctly formatted figure tag is returned") {
    val (figureString, errorList) = HtmlTagGenerator.buildFigure(sampleDataAttributes)
    figureString should equal("""<figure data-caption="Sample image" data-id="1" data-resource="image" data-url="http://localhost/1"></figure>""")
    errorList.isEmpty should be (true)
  }

  test("An error message is returned if an illegal attribute is used") {
    val dataAttributes = sampleDataAttributes + ("illegal-attribute" -> "")
    val (figureString, errorList) = HtmlTagGenerator.buildFigure(dataAttributes)
    figureString should equal("""<figure data-caption="Sample image" data-id="1" data-illegal-attribute="" data-resource="image" data-url="http://localhost/1"></figure>""")
    errorList.isEmpty should be (false)
    errorList.head.contains("This is a BUG:") should be (true)
  }

}
