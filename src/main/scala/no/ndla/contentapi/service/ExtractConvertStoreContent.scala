package no.ndla.contentapi.service

import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.model.{ImportStatus, NodeNotFoundException}
import no.ndla.contentapi.repository.ContentRepositoryComponent

import scala.util.{Failure, Success, Try}

trait ExtractConvertStoreContent {
  this: ExtractServiceComponent with ConverterServiceComponent with ContentRepositoryComponent =>

  val extractConvertStoreContent: ExtractConvertStoreContent

  class ExtractConvertStoreContent extends LazyLogging {
    def processNode(externalId: String, importStatus: ImportStatus = ImportStatus()): Try[(Long, ImportStatus)] = {

      if (importStatus.visitedNodes.contains(externalId))
        return contentRepository.getIdFromExternalId(externalId) match {
          case Some(id) => Success(id, importStatus)
          case None => Failure(NodeNotFoundException(s"Content with external id $externalId was not found"))
        }

      val node = extractService.getNodeData(externalId)

      node.contents.find(_.isMainNode) match {
        case Some(mainNode) => {
          val mainNodeId = mainNode.nid
          val (convertedNode, updatedImportStatus) = converterService.convertNode(node, importStatus)

          val newNodeId = contentRepository.exists(mainNodeId) match {
            case true => contentRepository.update(convertedNode, mainNodeId)
            case false => {
              contentRepository.insert(convertedNode, mainNodeId)
            }
          }

          Success((newNodeId, updatedImportStatus))
        }
        case None => Failure(NodeNotFoundException(s"$externalId is a translation; Could not find main node"))
      }
    }
  }

}
