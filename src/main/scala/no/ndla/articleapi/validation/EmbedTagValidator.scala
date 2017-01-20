package no.ndla.articleapi.validation

import no.ndla.articleapi.model.api.ValidationMessage
import no.ndla.articleapi.ArticleApiProperties.resourceHtmlEmbedTag
import no.ndla.articleapi.service.converters.HTMLCleaner
import org.jsoup.nodes.Element

import scala.collection.JavaConversions._

class EmbedTagValidator {

  def validateEmbedTag(fieldName: String, embed: Element): Option[ValidationMessage] = {
    if (embed.tagName != resourceHtmlEmbedTag)
      return None

    val attributes = embed.attributes().map(attr => attr.getKey -> attr.getValue).toMap
    val legalAttributeKeys = HTMLCleaner.legalAttributesForTag(embed.tagName).map(_.toString)

    if (!attributes.keySet.subsetOf(legalAttributeKeys))
      return Some(ValidationMessage(fieldName, s"An ${embed.tagName} html-tag contains illegal attributes. Allowed attributes are ${legalAttributeKeys.mkString(",")}"))
    None
  }

  private def containsRequiredAttributes(allAttributes: Map[String, String]) = {

  }
}
