/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.validation

import no.ndla.articleapi.ArticleApiProperties.resourceHtmlEmbedTag
import no.ndla.articleapi.UnitSuite
import no.ndla.articleapi.model.api.ValidationMessage
import no.ndla.articleapi.service.converters.{Attributes, ResourceType}

class EmbedTagValidatorTest extends UnitSuite {
  val embedTagValidator = new EmbedTagValidator()

  private def generateAttributes(attrs: Map[String, String]): String = {
    attrs.toList.sortBy(_._1.toString).map { case (key, value) => s"""$key="$value"""" }.mkString(" ")
  }

  private def generateTagWithAttrs(attrs: Map[Attributes.Value, String]): String = {
    val strAttrs = attrs map{ case (k, v) => k.toString -> v }
    s"""<$resourceHtmlEmbedTag ${generateAttributes(strAttrs)} />"""
  }

  private def findErrorByMessage(validationMessages: Seq[ValidationMessage], partialMessage: String) =
    validationMessages.find(_.message.contains(partialMessage))

  test("validate should return an empty sequence if input is not an embed tag") {
    embedTagValidator.validate("content", "<h1>hello</h1>") should equal (Seq())
  }

  test("validate should return validation error if embed tag uses illegal attributes") {
    val attrs = generateAttributes(Map(
      Attributes.DataResource.toString -> ResourceType.ExternalContent.toString,
      Attributes.DataUrl.toString -> "google.com", "illegalattr" -> "test"))

    val res = embedTagValidator.validate("content", s"""<$resourceHtmlEmbedTag $attrs />""")
    findErrorByMessage(res, "illegal attribute(s) 'illegalattr'").size should be (1)
  }

  test("validate should return validation error if an attribute contains HTML") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.ExternalContent.toString,
      Attributes.DataUrl -> "<i>google.com</i>"))
    val res = embedTagValidator.validate("content", tag)
    findErrorByMessage(res, "contains attributes with HTML: data-url").size should be (1)
  }

  test("validate should return validation error if embed tag does not contain required attributes for data-resource=image") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.Image.toString))
    val res = embedTagValidator.validate("content", tag)
    findErrorByMessage(res, s"data-resource=${ResourceType.Image} must contain the following attributes:").size should be (1)
  }

  test("validate should return no validation errors if image embed-tag is used correctly") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.Image.toString,
      Attributes.DataResource_Id -> "1234",
      Attributes.DataSize -> "fullbredde",
      Attributes.DataAlt -> "alternative text",
      Attributes.DataCaption -> "here is a rabbit",
      Attributes.DataAlign -> "left"
    ))
    embedTagValidator.validate("content", tag).size should be (0)
  }

  test("validate should return validation error if embed tag does not contain required attributes for data-resource=audio") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.Audio.toString))
    val res = embedTagValidator.validate("content", tag)
    findErrorByMessage(res, s"data-resource=${ResourceType.Audio} must contain the following attributes:").size should be (1)
  }

  test("validate should return no validation errors if audio embed-tag is used correctly") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.Audio.toString,
      Attributes.DataResource_Id -> "1234"
    ))
    embedTagValidator.validate("content", tag).size should be (0)
  }

  test("validate should return validation error if embed tag does not contain required attributes for data-resource=h5p") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.H5P.toString))
    val res = embedTagValidator.validate("content", tag)
    findErrorByMessage(res, s"data-resource=${ResourceType.H5P} must contain the following attributes:").size should be (1)
  }

  test("validate should return no validation errors if h5p embed-tag is used correctly") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.H5P.toString,
      Attributes.DataUrl -> "http://ndla.no/h5p/embed/1234"
    ))
    embedTagValidator.validate("content", tag).size should be (0)
  }

  test("validate should return validation error if embed tag does not contain required attributes for data-resource=brightcove") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.Brightcove.toString))
    val res = embedTagValidator.validate("content", tag)
    findErrorByMessage(res, s"data-resource=${ResourceType.Brightcove} must contain the following attributes:").size should be (1)
  }

  test("validate should return no validation errors if brightcove embed-tag is used correctly") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.Brightcove.toString,
      Attributes.DataCaption -> "here is a video",
      Attributes.DataVideoId -> "1234",
      Attributes.DataAccount -> "2183716",
      Attributes.DataPlayer -> "B28fas"))
    embedTagValidator.validate("content", tag).size should be (0)
  }

  test("validate should return validation error if embed tag does not contain required attributes for data-resource=content-link") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.ContentLink.toString))
    val res = embedTagValidator.validate("content", tag)
    findErrorByMessage(res, s"data-resource=${ResourceType.ContentLink} must contain the following attributes:").size should be (1)
  }

  test("validate should return no validation errors if content-link embed-tag is used correctly") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.ContentLink.toString,
      Attributes.DataContentId -> "54",
      Attributes.DataLinkText -> "interesting article"))
    embedTagValidator.validate("content", tag).size should be (0)
  }

  test("validate should return validation error if embed tag does not contain required attributes for data-resource=error") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.Error.toString))
    val res = embedTagValidator.validate("content", tag)
    findErrorByMessage(res, s"data-resource=${ResourceType.Error} must contain the following attributes:").size should be (1)
  }

  test("validate should return no validation errors if error embed-tag is used correctly") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.Error.toString,
      Attributes.DataMessage -> "interesting article"))
    embedTagValidator.validate("content", tag).size should be (0)
  }

  test("validate should return validation error if embed tag does not contain required attributes for data-resource=external") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.ExternalContent.toString))
    val res = embedTagValidator.validate("content", tag)
    findErrorByMessage(res, s"data-resource=${ResourceType.ExternalContent} must contain the following attributes:").size should be (1)
  }

  test("validate should return no validation errors if external embed-tag is used correctly") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.ExternalContent.toString,
      Attributes.DataUrl -> "https://www.youtube.com/watch?v=pCZeVTMEsik"))
    embedTagValidator.validate("content", tag).size should be (0)
  }

  test("validate should return validation error if embed tag does not contain required attributes for data-resource=nrk") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.NRKContent.toString))
    val res = embedTagValidator.validate("content", tag)
    findErrorByMessage(res, s"data-resource=${ResourceType.NRKContent} must contain the following attributes:").size should be (1)
  }

  test("validate should return no validation errors if nrk embed-tag is used correctly") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.NRKContent.toString,
      Attributes.DataNRKVideoId -> "123",
      Attributes.DataUrl -> "http://nrk.no/video/123"
    ))
    embedTagValidator.validate("content", tag).size should be (0)
  }

  test("validate should fail if only one optional attribute is specified") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.Image.toString,
      Attributes.DataAlt-> "123",
      Attributes.DataCaption-> "123",
      Attributes.DataResource_Id-> "123",
      Attributes.DataSize-> "full",
      Attributes.DataAlign-> "left",
      Attributes.DataUpperLeftX-> "0",
      Attributes.DataFocalX -> "0"
    ))
    embedTagValidator.validate("content", tag).size should be (2)
  }

  test("validate should succeed if all optional attributes are specified") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.Image.toString,
      Attributes.DataAlt-> "123",
      Attributes.DataCaption-> "123",
      Attributes.DataResource_Id-> "123",
      Attributes.DataSize-> "full",
      Attributes.DataAlign-> "left",
      Attributes.DataUpperLeftX-> "0",
      Attributes.DataUpperLeftY-> "0",
      Attributes.DataLowerRightX -> "1",
      Attributes.DataLowerRightY -> "1",
      Attributes.DataFocalX -> "0",
      Attributes.DataFocalY -> "1"
    ))

    embedTagValidator.validate("content", tag).size should be (0)
  }

  test("validate should succeed if all attributes in an attribute group are specified") {
    val tagWithFocal = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.Image.toString,
      Attributes.DataAlt-> "123",
      Attributes.DataCaption-> "123",
      Attributes.DataResource_Id-> "123",
      Attributes.DataSize-> "full",
      Attributes.DataAlign-> "left",
      Attributes.DataFocalX -> "0",
      Attributes.DataFocalY -> "1"
    ))

    embedTagValidator.validate("content", tagWithFocal).size should be (0)


    val tagWithCrop = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.Image.toString,
      Attributes.DataAlt-> "123",
      Attributes.DataCaption-> "123",
      Attributes.DataResource_Id-> "123",
      Attributes.DataSize-> "full",
      Attributes.DataAlign-> "left",
      Attributes.DataUpperLeftX-> "0",
      Attributes.DataUpperLeftY-> "0",
      Attributes.DataLowerRightX -> "1",
      Attributes.DataLowerRightY -> "1"
    ))

    embedTagValidator.validate("content", tagWithCrop).size should be (0)
  }

  test("validate should succeed if source url is from a legal domain") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.Prezi.toString,
      Attributes.DataUrl -> "https://prezi.com",
      Attributes.DataWidth -> "1",
      Attributes.DataHeight -> "1"
    ))

    embedTagValidator.validate("content", tag).size should be (0)
  }

  test("validate should fail if source url is from an illlegal domain") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.Prezi.toString,
      Attributes.DataUrl -> "https://evilprezi.com",
      Attributes.DataWidth -> "1",
      Attributes.DataHeight -> "1"
    ))

    val result = embedTagValidator.validate("content", tag)
    result.size should be (1)
    result.head.message.contains(s"can only contain ${Attributes.DataUrl} urls from the following domains:")  should be(true)
  }

}
