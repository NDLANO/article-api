/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.integration.ConverterModule.{jsoupDocumentToString, stringToJsoupDocument}
import no.ndla.articleapi.integration.{ConverterModule, LanguageContent}
import no.ndla.articleapi.model.api.ImportException
import no.ndla.articleapi.model.domain.{FootNoteItem, ImportStatus}
import no.ndla.articleapi.service.ExtractService
import org.jsoup.nodes.Element

import scala.annotation.tailrec
import scala.collection.JavaConversions._
import scala.util.{Failure, Success, Try}

trait BiblioConverter {
  this: ExtractService =>
  val biblioConverter: BiblioConverter

  class BiblioConverter extends ConverterModule with LazyLogging {
    def convert(content: LanguageContent, importStatus: ImportStatus): Try[(LanguageContent, ImportStatus)] = {
      val element = stringToJsoupDocument(content.content)
      val references = buildReferences(element)
      val referenceMap = references.isEmpty match {
        case true => return Success((content, importStatus))
        case false => buildReferenceMap(references)
      }

      referenceMap match {
        case Success((map, errors)) =>
          val finalImportStatus = ImportStatus(importStatus.messages ++ errors, importStatus.visitedNodes)
          Success((content.copy(content = jsoupDocumentToString(element), footNotes = Some(content.footNotes.getOrElse(map))), finalImportStatus))
        case Failure(x) => Failure(x)
      }
    }

    def buildReferences(element: Element): Seq[String] = {
      @tailrec def buildReferences(references: List[Element], referenceNodes: Seq[String], index: Int): Seq[String] = {
        if (references.isEmpty)
          return referenceNodes

        val id = references.head.id()
        val nodeId = id.substring(id.indexOf("-") + 1)

        references.head.removeAttr("id")
        references.head.attr("href", s"#ref_{$index}_cite")
        references.head.attr("name", s"ref_{$index}_sup")
        references.head.html(s"<sup>$index</sup>")

        buildReferences(references.tail, referenceNodes :+ nodeId, index + 1)
      }

      buildReferences(element.select("a[id~=biblio-(.*)]").toList, List(), 1)
    }

    def buildReferenceMap(references: Seq[String]): Try[(Map[String, FootNoteItem], Seq[String])] = {
      @tailrec def buildReferenceMap(references: Seq[String], biblioMap: Map[String, FootNoteItem], errorList: Seq[String], index: Int): Try[(Map[String, FootNoteItem], Seq[String])] = {
        if (references.isEmpty)
          return Success((biblioMap, errorList))

        buildReferenceItem(references.head) match {
          case (Some(fotNote)) => buildReferenceMap(references.tail, biblioMap + (s"ref_$index" -> fotNote), errorList, index + 1)
          case None => {
            val errorMessage = s"Could not find biblio with id ${references.head}"
            logger.error(errorMessage)
            Failure(ImportException(errorMessage))
          }
        }
      }
      buildReferenceMap(references, Map(), List(), 1)
    }

    def buildReferenceItem(nodeId: String): Option[FootNoteItem] =
      extractService.getBiblioMeta(nodeId).map(biblioMeta => FootNoteItem(biblioMeta.biblio, biblioMeta.authors))

  }
}

