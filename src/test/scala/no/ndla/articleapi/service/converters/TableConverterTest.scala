/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.converters

import no.ndla.articleapi.UnitSuite
import no.ndla.articleapi.integration.LanguageContent
import no.ndla.articleapi.model.domain.ImportStatus

class TableConverterTest extends UnitSuite {
  val nodeId = "1234"
  val sampleLanguageContent = LanguageContent(nodeId, nodeId, "sample content", "meta description", Some("en"))

  test("paragraphs are unwrapped if cell contains only one") {
    val table2x3 =
      s"""<table>
          |<tbody>
          |<tr>
          |<td><p>column</p></td>
          |<td><p>column</p><p>hey</p></td>
          |</tr>
          |</tbody>
          |</table>""".stripMargin.replace("\n", "")

    val table2x3ExpectedResult =
      s"""<table>
          |<tbody>
          |<tr>
          |<th>column</th>
          |<th><p>column</p><p>hey</p></th>
          |</tr>
          |</tbody>
          |</table>""".stripMargin.replace("\n", "")

    val initialContent = sampleLanguageContent.copy(content=table2x3)
    val (result, importStatus) = TableConverter.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content should equal(table2x3ExpectedResult)
  }

  test("The first table row (tr) is converted to table header th") {
    val table2x3 =
      s"""<table>
          |<tbody>
          |<tr>
          |<td>col 1</td>
          |<td>col 2</td>
          |</tr>
          |</tbody>
          |</table>""".stripMargin.replace("\n", "")

    val table2x3ExpectedResult =
      s"""<table>
          |<tbody>
          |<tr>
          |<th>col 1</th>
          |<th>col 2</th>
          |</tr>
          |</tbody>
          |</table>""".stripMargin.replace("\n", "")

    val initialContent = sampleLanguageContent.copy(content=table2x3)
    val (result, importStatus) = TableConverter.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content should equal(table2x3ExpectedResult)
  }

  test("Strong tags in table header are unwrapped") {
    val table2x3 =
      s"""<table>
          |<tbody>
          |<tr>
          |<th><strong>heading 1</strong></th>
          |<th><strong>heading 2</strong></th>
          |</tr>
          |<tr>
          |<td>col 1</td>
          |<td>col 2</td>
          |</tr>
          |</tbody>
          |</table>""".stripMargin.replace("\n", "")

    val table2x3ExpectedResult =
      s"""<table>
          |<tbody>
          |<tr>
          |<th>heading 1</th>
          |<th>heading 2</th>
          |</tr>
          |<tr>
          |<td>col 1</td>
          |<td>col 2</td>
          |</tr>
          |</tbody>
          |</table>""".stripMargin.replace("\n", "")

    val initialContent = sampleLanguageContent.copy(content=table2x3)
    val (result, importStatus) = TableConverter.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content should equal(table2x3ExpectedResult)
  }

}
