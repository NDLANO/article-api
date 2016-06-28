package no.ndla.contentapi.controller

import com.typesafe.scalalogging.LazyLogging
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.NativeJsonSupport
import org.scalatra.{Ok, ScalatraServlet}
import no.ndla.contentapi.business.SearchIndexer
import no.ndla.contentapi.model.{Error, ImportStatus}
import no.ndla.contentapi.ComponentRegistry.{contentRepository, converterService, extractService}
import no.ndla.logging.LoggerContext
import no.ndla.network.ApplicationUrl

trait InternController {
  val internController: InternController

  class InternController extends ScalatraServlet with NativeJsonSupport with LazyLogging {

    protected implicit override val jsonFormats: Formats = DefaultFormats

    before() {
      contentType = formats("json")
      LoggerContext.setCorrelationID(Option(request.getHeader("X-Correlation-ID")))
      ApplicationUrl.set(request)
    }

    after() {
      LoggerContext.clearCorrelationID()
      ApplicationUrl.clear()
    }

    post("/index") {
      Ok(SearchIndexer.indexDocuments())
    }

    post("/import/:node_id") {
      val nodeId = params("node_id")
      val node = extractService.importNode(nodeId)

      node.contents.find(_.isMainNode) match {
        case Some(mainNode) => {
          val mainNodeId = mainNode.nid
          val (convertedNode, importStatus) = converterService.convertNode(node)

          val newNodeId = contentRepository.exists(mainNodeId) match {
            case true => contentRepository.update(convertedNode, mainNodeId)
            case false => contentRepository.insert(convertedNode, mainNodeId)
          }

          val importedNodes = node.contents.map(_.nid).mkString(",")
          logger.info("Converted nodes {}", importedNodes)
          ImportStatus(importStatus.messages :+ s"Successfully imported nodes $importedNodes: $newNodeId")
        }
        case None => throw new Exception(s"$nodeId is a translation; Could not find main node")
      }
    }

    error {
      case t: Throwable => {
        logger.error(t.getMessage, t)
        halt(status = 500, body = Error(Error.GENERIC, t.getMessage))
      }
    }
  }
}