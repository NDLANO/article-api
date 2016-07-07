package no.ndla.contentapi.service.converters

import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.integration.{Biblio, BiblioAuthor, ConverterModule, LanguageContent}
import no.ndla.contentapi.model.{FootNoteItem, ImportStatus}
import no.ndla.contentapi.service.ExtractServiceComponent
import org.jsoup.nodes.Element

import scala.collection.JavaConversions._
import org.jsoup.parser.Tag

trait BiblioConverter {
  this: ExtractServiceComponent =>
  val biblioConverter: BiblioConverter

  class BiblioConverter extends ConverterModule with LazyLogging {
    def convert(content: LanguageContent): (LanguageContent, ImportStatus) = {
      val element = stringToJsoupDocument(content.content)

      val references = buildReferences(element)
      val (map, messages) = references.isEmpty match {
        case true => return (content, ImportStatus())
        case false => buildReferenceMap(references)
      }

      (content.copy(content=jsoupDocumentToString(element), footNotes=content.footNotes ++ map), ImportStatus(messages))
    }

    def buildReferences(element: Element): Seq[String] = {
      var referenceNodes = List[String]()
      val references = element.select("a[id~=biblio-(.*)]")

      for (i <- 0 until references.size) {
        val id = references(i).id()
        val nodeId = id.substring(id.indexOf("-") + 1)
        referenceNodes = referenceNodes :+ nodeId

        references(i).removeAttr("id")
        references(i).attr("data-resource", "footnote")
        references(i).attr("data-key", s"ref_${i + 1}")
        references(i).html(s"${i + 1}")
      }

      referenceNodes
    }

    def buildReferenceMap(references: Seq[String]): (Map[String, FootNoteItem], Seq[String]) = {
      var errorList = List[String]()
      var biblioMap = Map[String, FootNoteItem]()

      for ((nodeId, index) <- references.zipWithIndex) {
        buildReferenceItem(nodeId, index + 1) match {
          case (Some(fotNote)) => biblioMap += s"ref_${index + 1}" -> fotNote
          case None => {
            val errorMessage = s"Could not find biblio with id $nodeId"
            logger.warn(errorMessage)
            errorList :+ errorMessage
          }
        }
      }
      (biblioMap, errorList)
    }

    def buildReferenceItem(nodeId: String, num: Int): Option[FootNoteItem] = {
      extractService.getBiblio(nodeId) match {
        case Some(biblio) => {
          val authors = extractService.getBiblioAuthors(nodeId)
          Some(FootNoteItem(biblio, authors))
        }
        case None => None
      }
    }
  }
}


