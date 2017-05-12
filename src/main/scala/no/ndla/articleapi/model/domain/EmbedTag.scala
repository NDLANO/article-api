package no.ndla.articleapi.model.domain

import no.ndla.articleapi.model.api.ConfigurationException
import org.json4s.native.JsonMethods.parse
import org.json4s._
import no.ndla.articleapi.service.converters.{Attributes, ResourceType}

import scala.io.Source

object EmbedTag {
  def requiredAttributesForResourceType(resourceType: ResourceType.Value): Set[Attributes.Value] =
    requiredAttributesByResourceType.getOrElse(resourceType, requiredAttributesForAllResourceTypes).toSet

  private[domain] val requiredAttributesByResourceType: Map[ResourceType.Value, Seq[Attributes.Value]] = {
    val requiredAttrs = embedRulesToJson.get("requiredAttrsForResource").map(_.asInstanceOf[Map[String, Seq[String]]])

    requiredAttrs.getOrElse(Map.empty) map { case (key, value) => {
      val resourceTypeKey = ResourceType.valueOf(key).getOrElse(throw new ConfigurationException(s"Missing declaration of resource type '$key' in ResourceType enum"))
      resourceTypeKey -> value.map(attr => Attributes.valueOf(attr).getOrElse(throw new ConfigurationException(s"Missing declaration of attribute '$attr' in Attributes enum")))
    }}
  }

  private def embedRulesToJson: Map[String, Any] = convertJsonStr(Source.fromResource("embed-tag-rules.json").mkString)

  private def convertJsonStr(jsonStr: String): Map[String, Any] = {
    implicit val formats = org.json4s.DefaultFormats
    parse(jsonStr).extract[Map[String, Any]]
  }

  private val requiredAttributesForAllResourceTypes = Seq(Attributes.DataResource)
}
