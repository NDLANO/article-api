/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.service.{ExtractServiceComponent, StorageService}
import no.ndla.articleapi.integration.AudioApiClient
import no.ndla.articleapi.model.domain.{ImportStatus, RequiredLibrary}
import no.ndla.articleapi.service.converters.HtmlTagGenerator

trait AudioConverterModule  {
  this: ExtractServiceComponent with StorageService with AudioApiClient =>

  object AudioConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "audio"

    override def convert(content: ContentBrowser, visitedNodes: Seq[String]): (String, Seq[RequiredLibrary], ImportStatus) = {
      val nodeId = content.get("nid")
      logger.info(s"Converting audio with nid $nodeId")

      val (converted, status) = audioApiClient.getOrImportAudio(nodeId) match {
        case Some(id) => insertAudio(content, id)
        case None => insertFailedAudioImport(content)
      }

      (converted, Seq(), status ++ ImportStatus(Seq(), visitedNodes))
    }

    private def insertFailedAudioImport(content: ContentBrowser): (String, ImportStatus) = {
      val message = s"Failed to import audio with node id ${content.get("nid")}"
      logger.warn(message)
      (s"{Failed to import audio: ${content.get("nid")}}", ImportStatus(Seq(message), Seq()))
    }

    private def insertAudio(content: ContentBrowser, id: Long): (String, ImportStatus) = {
      val resourceAttributes = Map(
        "resource" -> "audio",
        "id" -> content.id.toString,
        "audio-id" -> id.toString
      )
      val (resource, errors) = HtmlTagGenerator.buildEmbedContent(resourceAttributes)
      (resource, ImportStatus(errors, Seq()))
    }

  }
}
