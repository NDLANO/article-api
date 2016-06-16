package no.ndla.contentapi.service.converters

import no.ndla.contentapi.integration.{ConverterModule, LanguageContent}
import no.ndla.contentapi.model.ImportStatus
import scala.collection.JavaConversions._

object DivTableConverter extends ConverterModule {
  def convert(content: LanguageContent): (LanguageContent, ImportStatus) = {
    val element = stringToJsoupDocument(content.content)
    for (div <- element.select("div.ndla_table, div.ndla_table_row, div.ndla_table_cell, div.ndla_table_cell_content")) {

      div.classNames() match {
        case table if table contains "ndla_table" => {
          div.tagName("table")
          div.classNames(Set("ndla_table"))
        }
        case row if row contains "ndla_table_row" => {
          div.tagName("tr")
          div.classNames(Set("ndla_table_row"))
        }
        case cell if cell contains "ndla_table_cell" => {
          div.tagName("td")
          div.classNames(Set("ndla_table_cell"))
        }
        case cellContent if cellContent contains "ndla_table_cell_content" => div.unwrap()
      }
    }

    (content.copy(content=jsoupDocumentToString(element)), ImportStatus())
  }
}
