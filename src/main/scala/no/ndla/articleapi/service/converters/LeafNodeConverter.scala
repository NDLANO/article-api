/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */


package no.ndla.articleapi.service.converters

import no.ndla.articleapi.ArticleApiProperties.{nodeTypeH5P, nodeTypeLenke, nodeTypeVideo}
import no.ndla.articleapi.integration.{ConverterModule, LanguageContent, MigrationApiClient, MigrationEmbedMeta}
import no.ndla.articleapi.model.domain.{ImportStatus, RequiredLibrary}
import no.ndla.articleapi.service.{ExtractService, TagsService}
import no.ndla.articleapi.service.converters.contentbrowser.{H5PConverterModule, LenkeConverterModule, VideoConverterModule}
import no.ndla.network.NdlaClient
import no.ndla.articleapi.integration.ConverterModule.{jsoupDocumentToString, stringToJsoupDocument}
import no.ndla.articleapi.model.api.ImportException

import scala.util.{Failure, Success, Try}

trait LeafNodeConverter {
  this: VideoConverterModule with LenkeConverterModule with HtmlTagGenerator with H5PConverterModule with ExtractService with MigrationApiClient with TagsService with NdlaClient with TagsService =>

  object LeafNodeConverter extends ConverterModule {

    def convert(content: LanguageContent, importStatus: ImportStatus): Try[(LanguageContent, ImportStatus)] = {
      val element = stringToJsoupDocument(content.content)
      val supportedContentTypes = Seq("video", "ekstern ressurs")

      val (embedHtml, requiredLibraries) = content.nodeType match {
        case `nodeTypeVideo` =>
          val (html, reqLib) = VideoConverter.toVideo("", content.nid)
          (html, Set(reqLib))
        case `nodeTypeH5P` =>
          val (html, reqLib) = H5PConverter.toH5PEmbed(content.nid)
          (html, Set(reqLib))
        case `nodeTypeLenke` if supportedContentTypes.contains(content.contentType.getOrElse("")) =>
          convertLenke(content.nid) match {
            case Success((html, requiredLib, _)) =>
              (html, requiredLib.toSet)
            case Failure(ex) => return Failure(ex)
          }
        case _ => return Failure(ImportException(s"Tried to import node with unsupported node/content -type: ${content.nodeType}/${content.contentType}"))
      }

      element.append(s"<section>$embedHtml</section>")

      Success(content.copy(content=jsoupDocumentToString(element), requiredLibraries=content.requiredLibraries ++ requiredLibraries), importStatus)
    }

    private def convertLenke(nodeId: String): Try[(String, Option[RequiredLibrary], Seq[String])] =
      extractService.getNodeEmbedMeta(nodeId).map(m => LenkeConverter.insertInlineLink(m.url.getOrElse(""), m.embedCode.getOrElse("")))

  }
}
