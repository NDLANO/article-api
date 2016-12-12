package no.ndla.articleapi.service.converters

import no.ndla.articleapi.UnitSuite
import no.ndla.articleapi.ArticleApiProperties.resourceHtmlEmbedTag
import no.ndla.articleapi.integration.LanguageContent
import no.ndla.articleapi.model.domain.ImportStatus

class HTMLCleanerTest extends UnitSuite {
  val nodeId = "1234"
  val defaultLanguageContent = LanguageContent(nodeId, nodeId, """<article><!-- this is a comment --><h1>heading<!-- comment --></h1></article>""", "metadescription", Some("en"))
  val defaultImportStatus = ImportStatus(Seq(), Seq())

  val defaultLanguageIngress = "Jeg er en ingress"
  val defaultLanguageIngressWithHtml = "<p>Jeg er en ingress</p>"

  test("That HTMLCleaner unwraps illegal attributes") {
    val initialContent = defaultLanguageContent.copy(content="""<body><article><h1 class="useless">heading<div style="width='0px'">hey</div></h1></article></body>""")
    val expectedResult = "<article><h1>heading<div>hey</div></h1></article>"
    val (result, status) = HTMLCleaner.convert(initialContent, defaultImportStatus)

    result.content.replace("\n", "") should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

  test("That HTMLCleaner unwraps illegal tags") {
    val initialContent = defaultLanguageContent.copy(content="""<article><h1>heading</h1><henriktag>hehe</henriktag></article>""")
    val expectedResult = "<article><h1>heading</h1>hehe</article>"
    val (result, status) = HTMLCleaner.convert(initialContent, defaultImportStatus)

    result.content.replace("\n", "") should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

  test("That HTMLCleaner removes comments") {
    val initialContent = defaultLanguageContent.copy(content="""<article><!-- this is a comment --><h1>heading<!-- comment --></h1></article>""")
    val expectedResult = "<article><h1>heading</h1></article>"
    val (result, status) = HTMLCleaner.convert(initialContent, defaultImportStatus)

    result.content.replace("\n", "") should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

  test("ingress is extracted when wrapped in <p> tags") {
    val content = s"""<section>
                   |<$resourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-id="1" data-resource="image" data-alt="Mobiltelefon sender SMS" />
                   |<p><strong>Medievanene er i endring.</br></strong></p>
                   |</section>
                   |<section>
                   |<h2>Mediehverdagen</h2>
                   |</section>""".stripMargin.replace("\n", "")

    val expectedContentResult =
      s"""<section>
         |<$resourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-id="1" data-resource="image" data-alt="Mobiltelefon sender SMS" />
         |</section>
         |<section>
         |<h2>Mediehverdagen</h2>
         |</section>""".stripMargin.replace("\n", "")
    val expectedIngressResult = "Medievanene er i endring."

    val (result, status) = HTMLCleaner.convert(defaultLanguageContent.copy(content=content), defaultImportStatus)

    result.content should equal(expectedContentResult)
    result.ingress should equal(Some(expectedIngressResult))
  }

  test("ingress text is not extracted when not present") {
    val content = s"""<section>
                    |<$resourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-id="1" data-resource="image" data-alt="Mobiltelefon sender SMS" />
                    |<h2>Mediehverdagen</h2>
                    |</section>""".stripMargin.replace("\n", "")
    val expectedContentResult =
      s"""<section>
         |<$resourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-id="1" data-resource="image" data-alt="Mobiltelefon sender SMS" />
         |<h2>Mediehverdagen</h2>
         |</section>""".stripMargin.replace("\n", "")
    val expectedIngressResult = None
    val (result, status) = HTMLCleaner.convert(defaultLanguageContent.copy(content=content), defaultImportStatus)

    result.content should equal(expectedContentResult)
    result.ingress should equal(expectedIngressResult)
    result.requiredLibraries.length should equal (0)
  }

  test("ingress image is not extracted when not present") {
    val content =
      """<section>
          |<p><strong>Du har sikkert opplevd rykter og usannheter</strong></p>
          |<ul>
          |<li><a href="#" title="Snopes">Snopes</a></li>
          |</ul>
        |</section>
      """.stripMargin.replace("\n", "")
    val expectedContentResult = """<section><ul><li><a href="#" title="Snopes">Snopes</a></li></ul></section>"""
    val expectedIngressResult = "Du har sikkert opplevd rykter og usannheter"
    val (result, status) = HTMLCleaner.convert(defaultLanguageContent.copy(content=content), defaultImportStatus)
    result.content should equal(expectedContentResult)
    result.ingress should equal(Some(expectedIngressResult))
    result.requiredLibraries.length should equal (0)
  }

  test("ingress text is extracted when wrapped in <strong> tags") {
    val content = s"""<section>
                    |<$resourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-id="1" data-resource="image" data-alt="Mobiltelefon sender SMS" />
                    |<strong>Medievanene er i endring.</strong>
                    |<h2>Mediehverdagen</h2>
                    |</section>""".stripMargin.replace("\n", "")
    val expectedContentResult =
      s"""<section>
        |<$resourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-id="1" data-resource="image" data-alt="Mobiltelefon sender SMS" />
        |<h2>Mediehverdagen</h2></section>""".stripMargin.replace("\n", "")
    val expectedIngressResult = "Medievanene er i endring."
    val (result, status) = HTMLCleaner.convert(defaultLanguageContent.copy(content=content), defaultImportStatus)

    result.content should equal(expectedContentResult)
    result.ingress should equal(Some(expectedIngressResult))
  }

  test("standalone text in a section is wrapped in <p> tags") {
    val content = s"""<section>
                      |Medievanene er i endring.
                      |<h2>Mediehverdagen</h2>
                      |</section>""".stripMargin.replace("\n", "")
    val expectedContentResult = s"""<section>
                      |<p>Medievanene er i endring.</p>
                      |<h2>Mediehverdagen</h2>
                      |</section>""".stripMargin.replace("\n", "")

    val (result, status) = HTMLCleaner.convert(defaultLanguageContent.copy(content=content), defaultImportStatus)

    result.content should equal(expectedContentResult)
  }
  test("blank standalone text in a section is not wrapped in <p> tags") {
    val content = s"""<section>Medievanene er i endring.<p>Noe innhold</p>  <h2>Mediehverdagen</h2></section>"""
    val expectedContentResult = s"""<section><p>Medievanene er i endring.</p><p>Noe innhold</p>  <h2>Mediehverdagen</h2></section>"""

    val (result, status) = HTMLCleaner.convert(defaultLanguageContent.copy(content=content), defaultImportStatus)

    result.content should equal(expectedContentResult)
  }

  test("That isAttributeKeyValid returns false for illegal attributes") {
    HTMLCleaner.isAttributeKeyValid("data-random-junk", "td") should equal (false)
  }

  test("That isAttributeKeyValid returns true for legal attributes") {
    HTMLCleaner.isAttributeKeyValid("align", "td") should equal (true)
  }

  test("That isTagValid returns false for illegal tags") {
    HTMLCleaner.isTagValid("yodawg") should equal (false)
  }

  test("That isTagValid returns true for legal attributes") {
    HTMLCleaner.isTagValid("section") should equal (true)
  }

  test("That HTMLCleaner do not insert ingress if already added from seperate table") {
    val content = s"""<section>
                      |<$resourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-id="1" data-resource="image" data-alt="Mobiltelefon sender SMS" />
                      |<strong>Medievanene er i endring.</strong>
                      |<h2>Mediehverdagen</h2>
                      |</section>""".stripMargin.replace("\n", "")
    val expectedContentResult = s"""<section>
          |<$resourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-id="1" data-resource="image" data-alt="Mobiltelefon sender SMS" />
          |<strong>Medievanene er i endring.</strong>
          |<h2>Mediehverdagen</h2>
          |</section>""".stripMargin.replace("\n", "")

    val notExpectedIngressResult = "Medievanene er i endring."
    val expectedIngressResult = "Jeg er en ingress"

    val (result, status) = HTMLCleaner.convert(defaultLanguageContent.copy(content=content, ingress = Some(defaultLanguageIngress)), defaultImportStatus)

    result.content should equal(expectedContentResult)
    result.ingress should equal(Some(expectedIngressResult))
    result.ingress should not equal Some(notExpectedIngressResult)
  }

  test("That HTMLCleaner removes all tags in ingress from seperate table") {
    val content = s"""<section>
                      |<$resourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-id="1" data-resource="image" data-alt="Mobiltelefon sender SMS" />
                      |<strong>Medievanene er i endring.</strong>
                      |<h2>Mediehverdagen</h2>
                      |</section>""".stripMargin.replace("\n", "")
    val expectedContentResult = s"""<section>
                                    |<$resourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-id="1" data-resource="image" data-alt="Mobiltelefon sender SMS" />
                                    |<strong>Medievanene er i endring.</strong>
                                    |<h2>Mediehverdagen</h2>
                                    |</section>""".stripMargin.replace("\n", "")

    val expectedIngressResult = "Jeg er en ingress"

    val (result, status) = HTMLCleaner.convert(defaultLanguageContent.copy(content=content, ingress = Some(defaultLanguageIngressWithHtml)), defaultImportStatus)

    result.content should equal(expectedContentResult)
    result.ingress should equal(Some(expectedIngressResult))
  }

  test("elements are replaced with data-caption text in meta description") {
    val shit = defaultLanguageContent.copy(content="", metaDescription=s"""Look at this image <$resourceHtmlEmbedTag data-resource="image" data-caption="image caption" />""")
    val (result, status) = HTMLCleaner.convert(shit, defaultImportStatus)

    result.metaDescription should equal ("Look at this image image caption")
  }
}
