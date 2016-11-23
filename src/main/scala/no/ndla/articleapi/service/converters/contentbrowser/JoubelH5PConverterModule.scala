/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.model.domain.{ImportStatus, RequiredLibrary}
import no.ndla.articleapi.service.ExtractServiceComponent
import no.ndla.articleapi.service.converters.HtmlTagGenerator

trait JoubelH5PConverterModule {
  this: ExtractServiceComponent =>

  object JoubelH5PConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "h5p_content"
    val JoubelH5PBaseUrl = "https://ndlah5p.joubel.com/node"

    override def convert(content: ContentBrowser, visitedNodes: Seq[String]): (String, Seq[RequiredLibrary], ImportStatus) = {
      val ndlaNodeId = content.get("nid")

      ValidH5PNodeIds.get(ndlaNodeId) match {
        case Some(joubelNodeId) => validH5PResource(joubelNodeId, content, visitedNodes)
        case None => invalidH5PResource(ndlaNodeId, content, visitedNodes)
      }
    }

    private def validH5PResource(joubelH5PNodeId: String, content: ContentBrowser, visitedNodes: Seq[String]) = {
      val ndlaNodeId = content.get("nid")
      logger.info(s"Converting h5p_content with nid $ndlaNodeId")
      val (replacement, embedContentUsageErrors) = HtmlTagGenerator.buildEmbedContent(Map(
        "resource" -> "h5p",
        "id" -> s"${content.id}",
        "url" -> s"$JoubelH5PBaseUrl/${ValidH5PNodeIds(ndlaNodeId)}") )
      (replacement, Seq(), ImportStatus(embedContentUsageErrors, visitedNodes))
    }

    private def invalidH5PResource(nodeId: String, content: ContentBrowser, visitedNodes: Seq[String]) = {
      val ndlaNodeId = content.get("nid")
      val message = s"H5P node $ndlaNodeId is not yet exported to new H5P service"
      logger.error(message)

      val (replacement, usageErrors) = HtmlTagGenerator.buildErrorContent(message, content.id.toString)
      (replacement, Seq(), ImportStatus(usageErrors :+ message, visitedNodes))
    }

  }

  private[contentbrowser] val ValidH5PNodeIds = Map(
    "160303" -> "1",
    "166925" -> "2",
    "157946" -> "3",
    "159786" -> "4",
    "158769" -> "5",
    "158794" -> "6",
    "163366" -> "7",
    "169536" -> "8",
    "158729" -> "9",
    "169894" -> "10",
    "155485" -> "11",
    "160176" -> "12",
    "162644" -> "13",
    "164475" -> "14",
    "167619" -> "15",
    "170366" -> "16",
    "160127" -> "17",
    "156653" -> "18",
    "167124" -> "19",
    "162262" -> "20",
    "160779" -> "21",
    "157945" -> "22",
    "160666" -> "23",
    "168908" -> "24",
    "161082" -> "25"
  )

}
