/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.converters

import no.ndla.validation.{Attributes, ResourceType}
import no.ndla.validation.EmbedTagRules.ResourceHtmlEmbedTag

trait HtmlTagGenerator {

  object HtmlTagGenerator {
    def buildEmbedContent(dataAttributes: Map[Attributes.Value, String]): String = {
      s"<$ResourceHtmlEmbedTag ${buildAttributesString(dataAttributes)} />"
    }

    def buildErrorContent(message: String): String =
      buildEmbedContent(Map(Attributes.DataResource -> ResourceType.Error.toString, Attributes.DataMessage -> message))

    def buildImageEmbedContent(caption: String, imageId: String, align: String, size: String, altText: String) = {
      val dataAttributes = Map(
        Attributes.DataResource -> ResourceType.Image.toString,
        Attributes.DataResource_Id -> imageId,
        Attributes.DataSize -> size,
        Attributes.DataAlt -> altText,
        Attributes.DataCaption -> caption,
        Attributes.DataAlign -> align)

      buildEmbedContent(dataAttributes)
    }

    def buildAudioEmbedContent(audioId: String) = {
      val dataAttributes = Map(Attributes.DataResource -> ResourceType.Audio.toString, Attributes.DataResource_Id -> audioId)
      buildEmbedContent(dataAttributes)
    }

    def buildH5PEmbedContent(url: String) = {
      val dataAttributes = Map(Attributes.DataResource -> ResourceType.H5P.toString, Attributes.DataUrl -> url)
      buildEmbedContent(dataAttributes)
    }

    def buildBrightCoveEmbedContent(caption: String, videoId: String, account: String, player: String) = {
      val dataAttributes = Map(
        Attributes.DataResource -> ResourceType.Brightcove.toString,
        Attributes.DataCaption -> caption,
        Attributes.DataVideoId -> videoId,
        Attributes.DataAccount -> account,
        Attributes.DataPlayer -> player
      )
      buildEmbedContent(dataAttributes)
    }

    def buildContentLinkEmbedContent(contentId: Long, linkText: String, linkContext: Option[String] = None): String = {
      val attributes = Map(
        Attributes.DataResource -> ResourceType.ContentLink.toString,
        Attributes.DataContentId -> s"$contentId",
        Attributes.DataLinkText -> linkText)

      val dataAttributes = linkContext.map(c => attributes.updated(Attributes.DataOpenIn, c))
        .getOrElse(attributes)

      buildEmbedContent(dataAttributes)
    }

    def buildConceptEmbedContent(conceptId: Long, linkText: String) = {
      val dataAttributes = Map(
        Attributes.DataResource -> ResourceType.ConceptLink.toString,
        Attributes.DataContentId -> s"$conceptId",
        Attributes.DataLinkText -> linkText)
      buildEmbedContent(dataAttributes)
    }

    def buildExternalInlineEmbedContent(url: String) = {
      val dataAttributes = Map(
        Attributes.DataResource -> ResourceType.ExternalContent.toString,
        Attributes.DataUrl -> url
      )
      buildEmbedContent(dataAttributes)
    }

    def buildNRKInlineVideoContent(nrkVideoId: String, url: String) = {
      val dataAttributes = Map(
        Attributes.DataResource -> ResourceType.NRKContent.toString,
        Attributes.DataNRKVideoId -> nrkVideoId,
        Attributes.DataUrl -> url
      )
      buildEmbedContent(dataAttributes)
    }

    def buildPreziInlineContent(url: String, width: String, height: String) = {
      val dataAttributes = Map(
        Attributes.DataResource -> ResourceType.Prezi.toString,
        Attributes.DataUrl -> url,
        Attributes.DataWidth -> width,
        Attributes.DataHeight -> height
      )
      buildEmbedContent(dataAttributes)
    }

    def buildCommoncraftInlineContent(url: String, width: String, height: String) = {
      val dataAttributes = Map(
        Attributes.DataResource -> ResourceType.Commoncraft.toString,
        Attributes.DataUrl -> url,
        Attributes.DataWidth -> width,
        Attributes.DataHeight -> height
      )
      buildEmbedContent(dataAttributes)
    }

    def buildNdlaFilmIundervisningInlineContent(url: String, width: String, height: String) = {
      val dataAttributes = Map(
        Attributes.DataResource -> ResourceType.NdlaFilmIundervisning.toString,
        Attributes.DataUrl -> url,
        Attributes.DataWidth -> width,
        Attributes.DataHeight -> height
      )
      buildEmbedContent(dataAttributes)
    }

    def buildKahootInlineContent(url: String, width: String, height: String) = {
      val dataAttributes = Map(
        Attributes.DataResource -> ResourceType.Kahoot.toString,
        Attributes.DataUrl -> url,
        Attributes.DataWidth -> width,
        Attributes.DataHeight -> height
      )
      buildEmbedContent(dataAttributes)
    }

    def buildKhanAcademyInlineContent(url: String, width: String, height: String) = {
      val dataAttributes = Map(
        Attributes.DataResource -> ResourceType.KhanAcademy.toString,
        Attributes.DataUrl -> url,
        Attributes.DataWidth -> width,
        Attributes.DataHeight -> height
      )
      buildEmbedContent(dataAttributes)
    }

    def buildDetailsSummaryContent(linkText: String, content: String) = {
      s"<details><summary>$linkText</summary>$content</details>"
    }

    def buildAnchor(href: String, anchorText: String, title: String, openInNewTab: Boolean): String = {
      val target = openInNewTab match {
        case true => Map(Attributes.Target -> "_blank", Attributes.Rel -> "noopener noreferrer")
        case false => Map[Attributes.Value, String]()
      }
      val attributes = Map(Attributes.Href -> href, Attributes.Title -> title) ++ target
      s"<a ${buildAttributesString(attributes)}>$anchorText</a>"
    }

    def buildFootNoteItem(title: String, `type`: String, year: String, edition: String, publisher: String, authors: Seq[String]) = {
      val attrs = Map(
        Attributes.DataResource -> ResourceType.FootNote.toString,
        Attributes.DataTitle -> title,
        Attributes.DataType -> `type`,
        Attributes.DataYear -> year,
        Attributes.DataEdition -> edition,
        Attributes.DataPublisher -> publisher,
        Attributes.DataAuthors -> authors.mkString(";")
      )
      buildEmbedContent(attrs)
    }

    def buildRelatedContent(articleIds: Set[Long]): String = {
      val attrs = Map(
        Attributes.DataResource -> ResourceType.RelatedContent.toString,
        Attributes.DataArticleIds -> articleIds.map(_.toString).mkString(",")
      )
      buildEmbedContent(attrs)
    }

    private def buildAttributesString(figureDataAttributeMap: Map[Attributes.Value, String]): String =
      figureDataAttributeMap.toList.sortBy(_._1.toString).map { case (key, value) => s"""$key="${value.trim}"""" }.mkString(" ")

  }

}

