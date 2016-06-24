package no.ndla.contentapi.service.converters

import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.integration.{ConverterModule, LanguageContent}
import no.ndla.contentapi.model.ImportStatus
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

      val errorMessages = convertBiblios(element) match {
        case Some((referenceList, errorMessages)) => {
          element.appendChild(referenceList)
          errorMessages
        }
        case None => List[String]()
      }

      (content.copy(content=jsoupDocumentToString(element)), ImportStatus(errorMessages))
    }

    def convertBiblios(element: Element): Option[(Element, Seq[String])] = {
      val references = buildReferences(element)

      references.isEmpty match {
        case true => None
        case false => {
          val (referenceList, errorMessages) = buildReferenceList(new Element(Tag.valueOf("ul"), ""), references)
          val finalReferenceList = new Element(Tag.valueOf("div"), "").appendChild(referenceList)
          Some(finalReferenceList, errorMessages)
        }
      }
    }

    def buildReferences(element: Element): Seq[String] = {
      var referenceNodes = List[String]()
      val references = element.select("a[id~=biblio-(.*)]")

      for (i <- 0 until references.size) {
        val id = references(i).id()
        val nodeId = id.substring(id.indexOf("-") + 1)
        referenceNodes = referenceNodes :+ nodeId

        references(i).attr("id", s"reference_${i + 1}")
        references(i).attr("href", s"#reference_list-$nodeId")
        references(i).html(s"${i + 1}")
      }

      referenceNodes
    }

    def buildReferenceList(el: Element, references: Seq[String]): (Element, Seq[String]) = {
      var errorList = List[String]()

      for ((nodeId, index) <- references.zipWithIndex) {
        val newEl = new Element(Tag.valueOf("li"), "")
        newEl.attr("id", s"reference_list-$nodeId")
        buildReferenceItem(nodeId, index + 1) match {
          case Some(html) => newEl.html(html)
          case None => {
            val errorMessage = s"Could not find biblio with id $nodeId"
            logger.warn(errorMessage)
            errorList :+ errorMessage
          }
        }
        el.appendChild(newEl)
      }

      (el, errorList)
    }

    def buildReferenceItem(nodeId: String, num: Int): Option[String] = {
      extractService.getBiblio(nodeId) match {
        case Some(biblio) => {
          val authorNames = extractService.getBiblioAuthors(nodeId).map(a => a.name).mkString(",")
          Some(s"""<a href="#reference_$num">$num.</a>
                   |${biblio.title} (${biblio.year}), $authorNames, Edition: ${biblio.edition}, Publisher: ${biblio.publisher}
           """.stripMargin)
        }
        case None => None
      }
    }
  }
}
