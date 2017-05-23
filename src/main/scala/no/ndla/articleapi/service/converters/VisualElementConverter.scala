/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.converters

import no.ndla.articleapi.integration.{AudioApiClient, ConverterModule, ImageApiClient, LanguageContent}
import no.ndla.articleapi.model.api.ImportException
import no.ndla.articleapi.model.domain.{ImportStatus, RequiredLibrary}
import no.ndla.articleapi.service.ExtractService
import no.ndla.articleapi.service.converters.contentbrowser.{AudioConverterModule, H5PConverterModule, ImageConverterModule, VideoConverterModule}

import scala.util.{Failure, Success, Try}

trait VisualElementConverter {
  this: ExtractService
    with ImageApiClient
    with AudioApiClient
    with H5PConverterModule
    with ImageConverterModule
    with VideoConverterModule
    with AudioConverterModule =>

  object VisualElementConverter extends ConverterModule {
    def convert(content: LanguageContent, importStatus: ImportStatus): Try[(LanguageContent, ImportStatus)] = {
      if (content.visualElement.isEmpty)
        return Success((content, importStatus))

      content.visualElement.flatMap(nodeIdToVisualElement) match {
        case Some((visual, requiredLibs)) =>
          val requiredLibraries = (content.requiredLibraries ++ requiredLibs).distinct
          Success(content.copy(visualElement=Some(visual), requiredLibraries=requiredLibraries), importStatus)
        case None => Failure(ImportException(s"Failed to convert node id ${content.visualElement.get}"))
      }
    }

    private def nodeIdToVisualElement(nodeId: String): Option[(String, Seq[RequiredLibrary])] = {
      val converters = Map[String, String => Option[(String, Seq[RequiredLibrary])]](
        ImageConverter.typeName -> toImage,
        AudioConverter.typeName -> toAudio,
        H5PConverter.typeName -> toH5P,
        VideoConverter.typeName -> toVideo
      )

      extractService.getNodeType(nodeId).flatMap(nodeType => {
        converters.get(nodeType.toLowerCase).flatMap(func => func(nodeId))
      })
    }

    private def toImage(nodeId: String): Option[(String, Seq[RequiredLibrary])] =
      ImageConverter.toImageEmbed(nodeId, "", "", "", "").map(imageEmbed => (imageEmbed, Seq.empty)).toOption

    private def toH5P(nodeId: String): Option[(String, Seq[RequiredLibrary])] = {
      val (embed, requiredLib) = H5PConverter.toH5PEmbed(nodeId)
      Some(embed, Seq(requiredLib))
    }

    private def toVideo(nodeId: String): Option[(String, Seq[RequiredLibrary])] = {
      val (embed, requiredLib) = VideoConverter.toVideo("", nodeId)
      Some(embed, Seq(requiredLib))
    }

    private def toAudio(nodeId: String): Option[(String, Seq[RequiredLibrary])] =
      AudioConverter.toAudio(nodeId).map(audioMebed => (audioMebed, Seq.empty)).toOption

  }
}
