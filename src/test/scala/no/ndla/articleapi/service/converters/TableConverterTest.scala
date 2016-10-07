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
import no.ndla.articleapi.model.ImportStatus

class TableConverterTest extends UnitSuite {
  val nodeId = "1234"
  val tableColumn = """<td>column</td>"""
  val tableColumnWithParagraph = "<td><p>column</p></td>"
  val tableColumnWithParagraphs = "<td><p>paragraph</p><p>another paragraph</p></td>"

  test("paragraphs are unwrapped if cell contains only one") {
    val table2x3 =
      s"""<table>
          |<tbody>
          |<tr>
          |$tableColumnWithParagraph
          |</tr>
          |</tbody>
          |</table>""".stripMargin.replace("\n", "")

    val table2x3ExpectedResult =
      s"""<table>
          |<tbody>
          |<th>
          |<td>column</td>
          |</th>
          |</tbody>
          |</table>""".stripMargin.replace("\n", "")

    val initialContent = LanguageContent(nodeId, nodeId, table2x3, Some("en"))
    val (result, importStatus) = TableConverter.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content should equal(table2x3ExpectedResult)
  }

  test("The first table row (tr) is converted to table header th") {
    val table2x3 =
      s"""<table>
          |<tbody>
          |<tr>
          |$tableColumnWithParagraphs
          |$tableColumnWithParagraphs
          |$tableColumnWithParagraphs
          |</tr>
          |</tbody>
          |</table>""".stripMargin.replace("\n", "")

    val table2x3ExpectedResult =
      s"""<table>
          |<tbody>
          |<th>
          |$tableColumnWithParagraphs
          |$tableColumnWithParagraphs
          |$tableColumnWithParagraphs
          |</th>
          |</tbody>
          |</table>""".stripMargin.replace("\n", "")

    val initialContent = LanguageContent(nodeId, nodeId, table2x3, Some("en"))
    val (result, importStatus) = TableConverter.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content should equal(table2x3ExpectedResult)
  }

}
