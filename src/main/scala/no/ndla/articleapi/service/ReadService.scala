/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import no.ndla.articleapi.model.{api, domain}
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.ArticleApiProperties.{externalApiUrls, resourceHtmlEmbedTag}
import no.ndla.articleapi.integration.ConverterModule.{jsoupDocumentToString, stringToJsoupDocument}
import no.ndla.articleapi.service.converters.{Attributes, ResourceType}
import org.jsoup.nodes.Element

import scala.collection.JavaConversions._

trait ReadService {
  this: ArticleRepository with ConverterService =>
  val readService: ReadService

  class ReadService {
    def withId(id: Long): Option[api.Article] =
      articleRepository.withId(id)
        .map(addUrlsAndIdsOnEmbedResources)
        .map(converterService.toApiArticle)

    private[service] def addUrlsAndIdsOnEmbedResources(article: domain.Article): domain.Article = {
      val articleWithUrls = article.content.map(addIdAndUrlOnResource)
      article.copy(content = articleWithUrls)
    }

    private[service] def addIdAndUrlOnResource(content: domain.ArticleContent): domain.ArticleContent = {
      val doc = stringToJsoupDocument(content.content)

      val embedTags = doc.select(s"$resourceHtmlEmbedTag").toList
      embedTags.zipWithIndex.foreach { case (embedTag, index) =>
        embedTag.attr(s"${Attributes.DataId}", s"$index")
        addUrlOnEmbedTag(embedTag)
      }

      content.copy(content = jsoupDocumentToString(doc))
    }

    private def addUrlOnEmbedTag(embedTag: Element) = {
      val resourceIdAttrName = Attributes.DataResource_Id.toString
      embedTag.hasAttr(resourceIdAttrName) match {
        case false =>
        case true => {
          val (resourceType, id) = (embedTag.attr(s"${Attributes.DataResource}"), embedTag.attr(resourceIdAttrName))
          embedTag.removeAttr(resourceIdAttrName)
          embedTag.attr(s"${Attributes.DataUrl}", s"${externalApiUrls(resourceType)}/$id")
        }
      }
    }

  }
}
