package no.ndla.articleapi.validation

import no.ndla.articleapi.model.api.ValidationMessage
import no.ndla.articleapi.ArticleApiProperties.resourceHtmlEmbedTag
import no.ndla.articleapi.integration.ConverterModule
import no.ndla.articleapi.service.converters.{Attributes, HTMLCleaner, ResourceType}
import org.jsoup.nodes.Element

import scala.collection.JavaConversions._

class EmbedTagValidator {
  def validate(fieldName: String, content: String): Seq[ValidationMessage] = {
    val document = ConverterModule.stringToJsoupDocument(content)
    document.select(s"$resourceHtmlEmbedTag").flatMap(validateEmbedTag(fieldName, _)).toList
  }

  private def validateEmbedTag(fieldName: String, embed: Element): Seq[ValidationMessage] = {
    if (embed.tagName != resourceHtmlEmbedTag)
      return Seq()

    val allAttributesOnTag = embed.attributes().map(attr => attr.getKey -> attr.getValue).toMap
    val legalAttributes = getLegalAttributesUsed(allAttributesOnTag)

    val validationErrors = attributesAreLegal(fieldName, allAttributesOnTag) ++
      attributesContainsNoHtml(fieldName, legalAttributes) ++
      verifyAttributeResource(fieldName, legalAttributes)

    validationErrors.toList
  }

  private def attributesAreLegal(fieldName: String, attributes: Map[String, String]): Option[ValidationMessage] = {
    val legalAttributeKeys = HTMLCleaner.legalAttributesForTag(resourceHtmlEmbedTag).map(_.toString)
    val illegalAttributesUsed: Set[String] = attributes.keySet diff legalAttributeKeys

    if (illegalAttributesUsed.nonEmpty) {
      Some(ValidationMessage(fieldName,
        s"An HTML tag '$resourceHtmlEmbedTag' contains an illegal attribute(s) '${illegalAttributesUsed.mkString(",")}'. Allowed attributes are ${legalAttributeKeys.mkString(",")}"))
    } else {
      None
    }
  }

  private def attributesContainsNoHtml(fieldName: String, attributes: Map[Attributes.Value, String]): Option[ValidationMessage] = {
    val attributesWithHtml = attributes.toList.filter{ case (_, value) =>
     new TextValidator(allowHtml=false).validate(fieldName, value).nonEmpty
    }.toMap.keySet

    if (attributesWithHtml.nonEmpty) {
      Some(ValidationMessage(fieldName, s"HTML tag '$resourceHtmlEmbedTag' contains attributes with HTML: ${attributesWithHtml.mkString(",")}"))
    } else {
      None
    }
  }

  private def verifyAttributeResource(fieldName: String, attributes: Map[Attributes.Value, String]): Seq[ValidationMessage] = {
    val attributeKeys = attributes.keySet
    if (!attributeKeys.contains(Attributes.DataResource)) {
      return ValidationMessage(fieldName, s"$resourceHtmlEmbedTag tags must contain a ${Attributes.DataResource} attribute") :: Nil
    }

    if (!ResourceType.all.contains(attributes(Attributes.DataResource))) {
      return Seq(ValidationMessage(fieldName, s"The ${Attributes.DataResource} attribute can only contain one of the following values: ${ResourceType.all.mkString(",")}"))
    }

    val resourceType = ResourceType.valueOf(attributes(Attributes.DataResource)).get
    val requiredAttributesForResourceType = requiredAttributesForAllResourceTypes ++
      Map(ResourceType.Image -> requiredAttributesForImageEmbedTag,
      ResourceType.Audio -> requiredAttributesForAudioEmbedTag,
      ResourceType.H5P -> requiredAttributesForH5PEmbedTag,
      ResourceType.Brightcove -> requiredAttributesForBrightCoveEmbedTag,
      ResourceType.ContentLink -> requiredAttributesForContentLink,
      ResourceType.Error -> requiredAttributesForError,
      ResourceType.ExternalContent -> requiredAttributesForExternalContent,
      ResourceType.NRKContent -> requiredAttributesForNrkContent
    ).getOrElse(resourceType, Set())

    verifyEmbedTagBasedOnResourceType(fieldName, requiredAttributesForResourceType, attributeKeys, resourceType)
  }

  private val requiredAttributesForAllResourceTypes = Set(Attributes.DataResource, Attributes.DataId)

  private val requiredAttributesForImageEmbedTag =
    Set(Attributes.DataResource_Id,
      Attributes.DataSize,
      Attributes.DataAlt,
      Attributes.DataCaption,
      Attributes.DataAlign)

  private val requiredAttributesForAudioEmbedTag =
    Set(Attributes.DataResource_Id)

  private val requiredAttributesForH5PEmbedTag =
    Set(Attributes.DataUrl)

  private val requiredAttributesForBrightCoveEmbedTag =
    Set(Attributes.DataCaption,
      Attributes.DataVideoId,
      Attributes.DataAccount,
      Attributes.DataPlayer)

  private val requiredAttributesForContentLink =
    Set(Attributes.DataContentId,
      Attributes.DataLinkText)

  private val requiredAttributesForError =
    Set(Attributes.DataMessage)

  private val requiredAttributesForExternalContent =
    Set(Attributes.DataUrl)

  private val requiredAttributesForNrkContent =
    Set(Attributes.DataId,
      Attributes.DataNRKVideoId,
      Attributes.DataUrl)

  private def verifyEmbedTagBasedOnResourceType(fieldName: String, requiredAttributes: Set[Attributes.Value], actualAttributes: Set[Attributes.Value], resourceType: ResourceType.Value): Seq[ValidationMessage] = {
    val missingAttributes = getMissingAttributes(requiredAttributes, actualAttributes)
    val illegalAttributes = getMissingAttributes(actualAttributes, requiredAttributes)

    val partialErrorMessage = s"An $resourceHtmlEmbedTag HTML tag with ${Attributes.DataResource}=$resourceType"
    missingAttributes.map(missingAttributes => ValidationMessage(fieldName,  s"$partialErrorMessage must contain the following attributes: ${requiredAttributes.mkString(",")}. Missing: ${missingAttributes.mkString(",")}")).toList ++
    illegalAttributes.map(illegalAttributes => ValidationMessage(fieldName, s"$partialErrorMessage can not contain any of the following attributes: ${illegalAttributes.mkString(",")}"))
  }

  private def getMissingAttributes(requiredAttributes: Set[Attributes.Value], attributeKeys: Set[Attributes.Value]) = {
    val missing = requiredAttributes diff attributeKeys
    missing.headOption.map(_ => missing)
  }

  private def getLegalAttributesUsed(allAttributes: Map[String, String]): Map[Attributes.Value, String] = {
    val legalAttributeKeys = HTMLCleaner.legalAttributesForTag(resourceHtmlEmbedTag).map(_.toString)

    allAttributes.filter {case (key, _) => legalAttributeKeys.contains(key)}
      .map { case (key, value) => Attributes.valueOf(key).get -> value
    }
  }

}
