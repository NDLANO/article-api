package no.ndla.articleapi.model.domain

import no.ndla.articleapi.model.api.ConfigurationException
import no.ndla.articleapi.service.converters.{Attributes, ResourceType}
import org.json4s.native.JsonMethods.parse
import org.json4s._
import scala.language.postfixOps
import scala.io.Source

object EmbedTag {
  case class EmbedThings(attrsForResource: Map[ResourceType.Value, EmbedTagAttributeRules])
  case class EmbedTagAttributeRules(required: Set[Attributes.Value], optional: Set[Attributes.Value]) {
    lazy val all: Set[Attributes.Value] = required ++ optional
  }

  private[domain] lazy val attributeRules: Map[ResourceType.Value, EmbedTagAttributeRules] = embedRulesToJson

  lazy val allEmbedTagAttributes: Set[Attributes.Value] = attributeRules.flatMap { case (_ , attrRules)  => attrRules.all } toSet

  def attributesForResourceType(resourceType: ResourceType.Value): EmbedTagAttributeRules = attributeRules(resourceType)

  private def embedRulesToJson = {
    val requiredAttrs = convertJsonStr(Source.fromResource("embed-tag-rules.json").mkString)
      .get("attrsForResource").map(_.asInstanceOf[Map[String, Map[String, Seq[String]] ]])

    def toEmbedTagAttributeRules(map: Map[String, Seq[String]]) = {
      EmbedTagAttributeRules(
        map("required").flatMap(Attributes.valueOf).toSet,
        map.get("optional").map(x => x.flatMap(Attributes.valueOf)).getOrElse(Seq.empty).toSet
      )
    }

    def strToResourceType(str: String): ResourceType.Value =
      ResourceType.valueOf(str).getOrElse(throw new ConfigurationException(s"Missing declaration of resource type '$str' in ResourceType enum"))

    requiredAttrs.get.map {
      case (resourceType, attrRules) => strToResourceType(resourceType) -> toEmbedTagAttributeRules(attrRules)
    }
  }

  private def convertJsonStr(jsonStr: String): Map[String, Any] = {
    implicit val formats = org.json4s.DefaultFormats
    parse(jsonStr).extract[Map[String, Any]]
  }
}
