/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.converters

import no.ndla.articleapi.integration.LanguageIngress
import no.ndla.articleapi.model.domain.ImportStatus
import no.ndla.articleapi.{TestData, TestEnvironment, UnitSuite}

import scala.util.Success

class MathMLConverterTest extends UnitSuite with TestEnvironment {
  val nodeId = "1234"
  val defaultImportStatus = ImportStatus(Seq(), Seq())

  val defaultLanguageIngress = LanguageIngress("Jeg er en ingress", None)
  val defaultLanguageIngressWithHtml = LanguageIngress("<p>Jeg er en ingress</p>", None)


  test("an xmlns attribute should be added to MathML math tags") {
    val originalContent = """<math><mi>P</mi></math>"""
    val expectedContent = """<math xmlns="http://www.w3.org/1998/Math/MathML"><mi>P</mi></math>"""
    val content = TestData.sampleContent.copy(content=originalContent)
    val Success((result, _)) = MathMLConverter.convert(content, defaultImportStatus)

    result.content should equal (expectedContent)
  }

  test("nbsp in MathML tags should be converted to space") {
    val originalContent = "<math>\u00a0<mi>P\u00a0</mi></math>"
    val expectedContent = """<math xmlns="http://www.w3.org/1998/Math/MathML"> <mi>P </mi></math>"""
    val content = TestData.sampleContent.copy(content=originalContent)
    val Success((result, _)) = MathMLConverter.convert(content, defaultImportStatus)

    result.content should equal (expectedContent)
  }

}
