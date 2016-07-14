package no.ndla.contentapi.service

import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.ContentApiProperties.maxConvertionRounds
import no.ndla.contentapi.integration.NodeToConvert
import no.ndla.contentapi.model.{Content, ContentInformation, ImportStatus}
import no.ndla.contentapi.ContentApiProperties.permittedHTMLTags
import org.jsoup.Jsoup

import scala.annotation.tailrec
import scala.collection.JavaConversions._

trait ConverterServiceComponent {
  this: ConverterModules =>
  val converterService: ConverterService

  class ConverterService extends LazyLogging {
    def convertNode(nodeToConvert: NodeToConvert): (ContentInformation, ImportStatus) = {
      @tailrec def convertNode(nodeToConvert: NodeToConvert, maxRoundsLeft: Int, importStatus: ImportStatus = ImportStatus()): (ContentInformation, ImportStatus) = {
        if (maxRoundsLeft == 0) {
          val message = "Maximum number of converter rounds reached; Some content might not be converted"
          logger.warn(message)
          return (nodeToConvert.asContentInformation, ImportStatus(importStatus.messages :+ message))
        }

        val (updatedContent, updatedStatus) = convert(nodeToConvert)

        // If this converting round did not yield any changes to the content, this node is finished (case true)
        // If changes were made during this convertion, we run the converters again (case false)
        updatedContent == nodeToConvert match {
          case true => (updatedContent.asContentInformation, ImportStatus(importStatus.messages ++ updatedStatus.messages))
          case false => convertNode(updatedContent, maxRoundsLeft - 1, ImportStatus(importStatus.messages ++ updatedStatus.messages))
        }
      }

      val (contentInformation, importStatus) = convertNode(nodeToConvert, maxConvertionRounds)
      val illegalTagsMessages = checkIllegalTags(contentInformation.content).map(x => s"Illegal tag in article: $x")
      (contentInformation, ImportStatus(importStatus.messages ++ illegalTagsMessages))
    }

    private def convert(nodeToConvert: NodeToConvert): (NodeToConvert, ImportStatus) =
      converterModules.foldLeft((nodeToConvert, ImportStatus()))((element, converter) => {
        val (updatedNodeToConvert, importStatus) = element
        converter.convert(updatedNodeToConvert, importStatus)
      })

    def checkIllegalTags(contents: Seq[Content]): Seq[String] = {
      contents.foldLeft(Seq[String]())((list, content) => {
        list ++ Jsoup.parseBodyFragment(content.content).select("article").select("*").toList
          .map(x => x.tagName).distinct // get a list of all html tags in the article
          .filter(x => !permittedHTMLTags.contains(x)) // get all tags which is not defined in the permittedHTMLTags list
      }).distinct
    }

  }
}
