package no.ndla.articleapi.model.domain

import no.ndla.articleapi.model.api.ConfigurationException
import no.ndla.articleapi.service.converters.{Attributes, ResourceType}
import org.json4s.native.JsonMethods.parse
import org.json4s._
import scala.language.postfixOps
import scala.io.Source

object EmbedTag {
  case class EmbedThings(attrsForResource: Map[ResourceType.Value, EmbedTagAttributeRules])
  case class EmbedTagAttributeRules(required: Set[Attributes.Value], optional: Seq[Set[Attributes.Value]], validSrcDomains: Set[String]) {
    lazy val all: Set[Attributes.Value] = required ++ optional.flatten
  }

  private[domain] lazy val attributeRules: Map[ResourceType.Value, EmbedTagAttributeRules] = embedRulesToJson

  lazy val allEmbedTagAttributes: Set[Attributes.Value] = attributeRules.flatMap { case (_ , attrRules)  => attrRules.all } toSet

  def attributesForResourceType(resourceType: ResourceType.Value): EmbedTagAttributeRules = attributeRules(resourceType)

  private def embedRulesToJson = {
    val attrs = convertJsonStr(Source.fromResource("embed-tag-rules.json").mkString)
      .get("attrsForResource").map(_.asInstanceOf[Map[String, Map[String, Any]]])

    def toEmbedTagAttributeRules(map: Map[String, Any]) = {
      val optionalAttrs: List[List[Attributes.Value]] = map.get("optional")
        .map(_.asInstanceOf[List[List[String]]].map(_.flatMap(Attributes.valueOf))).getOrElse(List.empty)
      val validSrcDomains: Seq[String] = map.get("validSrcDomains").map(_.asInstanceOf[Seq[String]]).getOrElse(Seq.empty)

      EmbedTagAttributeRules(
        map("required").asInstanceOf[Seq[String]].flatMap(Attributes.valueOf).toSet,
        optionalAttrs.map(_.toSet),
        validSrcDomains.toSet
      )
    }

    def strToResourceType(str: String): ResourceType.Value =
      ResourceType.valueOf(str).getOrElse(throw new ConfigurationException(s"Missing declaration of resource type '$str' in ResourceType enum"))

    attrs.get.map {
      case (resourceType, attrRules) => strToResourceType(resourceType) -> toEmbedTagAttributeRules(attrRules)
    }
  }

  private def convertJsonStr(jsonStr: String): Map[String, Any] = {
    implicit val formats = org.json4s.DefaultFormats
    parse(jsonStr).extract[Map[String, Any]]
  }
}
