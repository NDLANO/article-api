package no.ndla.contentapi.controller

import com.typesafe.scalalogging.LazyLogging
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.NativeJsonSupport
import org.scalatra.{Ok, ScalatraServlet}
import no.ndla.contentapi.business.SearchIndexer
import no.ndla.contentapi.model.{Error, ImportStatus, NodeNotFoundException}
import no.ndla.contentapi.repository.ContentRepositoryComponent
import no.ndla.contentapi.service.{ConverterServiceComponent, ExtractServiceComponent}
import no.ndla.logging.LoggerContext
import no.ndla.network.ApplicationUrl

trait InternController {
  this: ExtractServiceComponent with ConverterServiceComponent with ContentRepositoryComponent =>
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

    error {
      case t: Throwable => {
        logger.error(t.getMessage, t)
        halt(status = 500, body = Error(Error.GENERIC, t.getMessage))
      }
    }

    post("/index") {
      Ok(SearchIndexer.indexDocuments())
    }

    post("/import/:node_id") {
      val nodeId = params("node_id")

      val node = extractService.getNodeData(nodeId)
      val nodesToImport = node.contents.map(_.nid).mkString(",")

      logger.info("Converting nodes {}", nodesToImport)
      node.contents.find(_.isMainNode) match {
        case Some(mainNode) => {
          val mainNodeId = mainNode.nid
          val (convertedNode, importStatus) = converterService.convertNode(node)

          val newNodeId = contentRepository.exists(mainNodeId) match {
            case true => contentRepository.update(convertedNode, mainNodeId)
            case false => contentRepository.insert(convertedNode, mainNodeId)
          }

          ImportStatus(importStatus.messages :+ s"Successfully imported nodes $nodesToImport: $newNodeId")
        }
        case None => throw new NodeNotFoundException(s"$nodeId is a translation; Could not find main node")
      }
    }
  }
}