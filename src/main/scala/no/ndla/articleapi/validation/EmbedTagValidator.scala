/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.validation

import no.ndla.articleapi.model.api.ValidationMessage
import no.ndla.articleapi.ArticleApiProperties.resourceHtmlEmbedTag
import no.ndla.articleapi.integration.ConverterModule
import no.ndla.articleapi.model.domain.EmbedTag
import no.ndla.articleapi.model.domain.EmbedTag.EmbedTagAttributeRules
import no.ndla.articleapi.service.converters.{Attributes, HTMLCleaner, ResourceType}
import org.jsoup.nodes.Element
import com.netaporter.uri.dsl._
import scala.collection.JavaConverters._

class EmbedTagValidator {
  def validate(fieldName: String, content: String): Seq[ValidationMessage] = {
    val document = ConverterModule.stringToJsoupDocument(content)
    document.select(s"$resourceHtmlEmbedTag").asScala.flatMap(validateEmbedTag(fieldName, _)).toList
  }

  private def validateEmbedTag(fieldName: String, embed: Element): Seq[ValidationMessage] = {
    if (embed.tagName != resourceHtmlEmbedTag)
      return Seq()

    val allAttributesOnTag = embed.attributes().asScala.map(attr => attr.getKey -> attr.getValue).toMap
    val legalAttributes = getLegalAttributesUsed(allAttributesOnTag)

    val validationErrors = attributesAreLegal(fieldName, allAttributesOnTag) ++
      attributesContainsNoHtml(fieldName, legalAttributes) ++
      verifyAttributeResource(fieldName, legalAttributes)

    validationErrors.toList
  }

  private def attributesAreLegal(fieldName: String, attributes: Map[String, String]): Option[ValidationMessage] = {
    val legalAttributeKeys = HTMLCleaner.legalAttributesForTag(resourceHtmlEmbedTag)
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
    val attributeRulesForTag = EmbedTag.attributesForResourceType(resourceType)

    verifyEmbedTagBasedOnResourceType(fieldName, attributeRulesForTag, attributeKeys, resourceType) ++
      verifyOptionals(fieldName, attributeRulesForTag, attributeKeys, resourceType) ++
      verifySourceUrl(fieldName, attributeRulesForTag, attributes, resourceType)
  }

  private def verifyEmbedTagBasedOnResourceType(fieldName: String, attrRules: EmbedTagAttributeRules, actualAttributes: Set[Attributes.Value], resourceType: ResourceType.Value): Seq[ValidationMessage] = {
    val missingAttributes = getMissingAttributes(attrRules.required, actualAttributes)
    val illegalAttributes = getMissingAttributes(actualAttributes, attrRules.all)

    val partialErrorMessage = s"An $resourceHtmlEmbedTag HTML tag with ${Attributes.DataResource}=$resourceType"
    missingAttributes.map(missingAttributes => ValidationMessage(fieldName, s"$partialErrorMessage must contain the following attributes: ${attrRules.required.mkString(",")}. " +
      s"Optional attributes are: ${attrRules.optional.mkString(",")}. " +
      s"Missing: ${missingAttributes.mkString(",")}")).toList ++
      illegalAttributes.map(illegalAttributes => ValidationMessage(fieldName, s"$partialErrorMessage can not contain any of the following attributes: ${illegalAttributes.mkString(",")}"))
  }

  private def verifyOptionals(fieldName: String,
                              attrsRules: EmbedTagAttributeRules,
                              actualAttributes: Set[Attributes.Value],
                              resourceType: ResourceType.Value): Seq[ValidationMessage] = {

    val usedOptionalAttr = actualAttributes.intersect(attrsRules.optional.flatten.toSet)
    val a = usedOptionalAttr.flatMap(attr => {
      val attrRuleGroup = attrsRules.optional.find(_.contains(attr))
      attrRuleGroup.map(attrRules => verifyUsedAttributesAgainstAttrRules(fieldName, attrRules, usedOptionalAttr, resourceType))
    }).toSeq
    a.flatten
  }

  private def verifyUsedAttributesAgainstAttrRules(fieldName: String,
                                                   attrRules: Set[Attributes.Value],
                                                   usedOptionalAttrs: Set[Attributes.Value],
                                                   resourceType: ResourceType.Value): Seq[ValidationMessage] = {
    val usedOptionalAttrsInCurrentGroup = usedOptionalAttrs.intersect(attrRules)
    usedOptionalAttrsInCurrentGroup.isEmpty match {
      case false if usedOptionalAttrsInCurrentGroup != attrRules =>
        val missingAttrs = attrRules.diff(usedOptionalAttrs).mkString(",")
        Seq(ValidationMessage(fieldName,
          s"An $resourceHtmlEmbedTag HTML tag with ${Attributes.DataResource}=$resourceType must contain all or none of the optional attributes (${attrRules.mkString(",")}). Missing $missingAttrs"
        ))
      case _ => Seq.empty
    }
  }

  private def verifySourceUrl(fieldName: String, attrs: EmbedTagAttributeRules, usedAttributes: Map[Attributes.Value, String], resourceType: ResourceType.Value): Seq[ValidationMessage] = {
    usedAttributes.get(Attributes.DataUrl) match {
      case Some(url) if attrs.validSrcDomains.nonEmpty && !attrs.validSrcDomains.exists(url.host.getOrElse("").matches(_)) =>
        Seq(ValidationMessage(fieldName,
          s"An $resourceHtmlEmbedTag HTML tag with ${Attributes.DataResource}=$resourceType can only contain ${Attributes.DataUrl} urls from the following domains: ${attrs.validSrcDomains.mkString(",")}"))
      case _ => Seq.empty
    }
  }

  private def getMissingAttributes(requiredAttributes: Set[Attributes.Value], attributeKeys: Set[Attributes.Value]) = {
    val missing = requiredAttributes diff attributeKeys
    missing.headOption.map(_ => missing)
  }

  private def getLegalAttributesUsed(allAttributes: Map[String, String]): Map[Attributes.Value, String] = {
    val legalAttributeKeys = HTMLCleaner.legalAttributesForTag(resourceHtmlEmbedTag)

    allAttributes.filter { case (key, _) => legalAttributeKeys.contains(key) }
      .map { case (key, value) => Attributes.valueOf(key).get -> value }
  }

}
