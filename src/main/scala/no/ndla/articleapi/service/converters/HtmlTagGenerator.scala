/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.converters

import no.ndla.articleapi.ArticleApiProperties._

trait HtmlTagGenerator {

  object HtmlTagGenerator {
    def buildEmbedContent(dataAttributes: Map[Attributes.Value, String]): String = {
      s"<$resourceHtmlEmbedTag ${buildAttributesString(dataAttributes)} />"
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

    def buildContentLinkEmbedContent(contentId: Long, linkText: String): String = {
      val dataAttributes = Map(
        Attributes.DataResource -> ResourceType.ContentLink.toString,
        Attributes.DataContentId -> s"$contentId",
        Attributes.DataLinkText -> linkText)
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

object ResourceType extends Enumeration {
  val Error = Value("error")
  val Image = Value("image")
  val Audio = Value("audio")
  val H5P = Value("h5p")
  val Brightcove = Value("brightcove")
  val ContentLink = Value("content-link")
  val ExternalContent = Value("external")
  val NRKContent = Value("nrk")
  val ConceptLink = Value("concept")
  val Prezi = Value("prezi")
  val Commoncraft = Value("commoncraft")
  val NdlaFilmIundervisning = Value("ndla-filmiundervisning")
  val Kahoot = Value("kahoot")
  val KhanAcademy = Value("khan-academy")
  val FootNote = Value("footnote")
  val RelatedContent = Value("related-content")

  def all: Set[String] = ResourceType.values.map(_.toString)

  def valueOf(s: String): Option[ResourceType.Value] = {
    ResourceType.values.find(_.toString == s)
  }
}

object Attributes extends Enumeration {
  val DataUrl = Value("data-url")
  val DataAlt = Value("data-alt")
  val DataSize = Value("data-size")
  val DataAlign = Value("data-align")
  val DataWidth = Value("data-width")
  val DataHeight = Value("data-height")
  val DataPlayer = Value("data-player")
  val DataMessage = Value("data-message")
  val DataCaption = Value("data-caption")
  val DataAccount = Value("data-account")
  val DataVideoId = Value("data-videoid")
  val DataResource = Value("data-resource")
  val DataLinkText = Value("data-link-text")
  val DataContentId = Value("data-content-id")
  val DataNRKVideoId = Value("data-nrk-video-id")
  val DataResource_Id = Value("data-resource_id")
  val DataTitle = Value("data-title")
  val DataType = Value("data-type")
  val DataYear = Value("data-year")
  val DataEdition = Value("data-edition")
  val DataPublisher = Value("data-publisher")
  val DataAuthors = Value("data-authors")
  val DataArticleIds = Value("data-article-ids")

  val DataUpperLeftY =  Value("data-upper-left-y")
  val DataUpperLeftX = Value("data-upper-left-x")
  val DataLowerRightY = Value("data-lower-right-y")
  val DataLowerRightX = Value("data-lower-right-x")
  val DataFocalX = Value("data-focal-x")
  val DataFocalY = Value("data-focal-y")

  val XMLNsAttribute = Value("xmlns")

  val Href = Value("href")
  val Title = Value("title")
  val Align = Value("align")
  val Valign = Value("valign")
  val Target = Value("target")
  val Rel = Value("rel")


  def all: Set[String] = Attributes.values.map(_.toString)

  def valueOf(s: String): Option[Attributes.Value] = {
    Attributes.values.find(_.toString == s)
  }
}

