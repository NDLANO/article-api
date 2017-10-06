/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import no.ndla.articleapi.ArticleApiProperties.{externalApiUrls, resourceHtmlEmbedTag}
import no.ndla.articleapi.caching.MemoizeAutoRenew
import no.ndla.articleapi.integration.ConverterModule.{jsoupDocumentToString, stringToJsoupDocument}
import no.ndla.articleapi.model.api
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.model.domain.Language._
import no.ndla.articleapi.repository.{ArticleRepository, ConceptRepository}
import no.ndla.articleapi.service.converters.Attributes
import org.jsoup.nodes.Element

import scala.collection.JavaConverters._

trait ReadService {
  this: ArticleRepository with ConceptRepository with ConverterService =>
  val readService: ReadService

  class ReadService {
    def getInternalIdByExternalId(externalId: Long): Option[api.ArticleIdV2] =
      articleRepository.getIdFromExternalId(externalId.toString()) match {
        case Some(id) => Some(api.ArticleIdV2(id))
        case _ => None
      }

    def articleWithId(id: Long): Option[api.Article] =
      articleRepository.withId(id)
        .map(addUrlsOnEmbedResources)
        .map(converterService.toApiArticle)

    def withIdV2(id: Long, language: String): Option[api.ArticleV2] = {
      articleRepository.withId(id)
        .map(addUrlsOnEmbedResources)
        .flatMap(article => converterService.toApiArticleV2(article, language))
    }

    private[service] def addUrlsOnEmbedResources(article: Article): Article = {
      val articleWithUrls = article.content.map(content => content.copy(content=addUrlOnResource(content.content)))
      val visualElementWithUrls = article.visualElement.map(visual => visual.copy(resource=addUrlOnResource(visual.resource)))

      article.copy(content = articleWithUrls, visualElement = visualElementWithUrls)
    }

    def getNMostUsedTags(n: Int, language: String): Option[Seq[api.ArticleTag]] = {
      val tagUsageMap = getTagUsageMap()
      val supportedLanguages = tagUsageMap.keys.toSeq.distinct
      val searchLanguage = getSearchLanguage(language, supportedLanguages)

      if (supportedLanguages.isEmpty || (!supportedLanguages.contains(language) && language != AllLanguages)) return None

      Some(tagUsageMap
        .filterKeys(lang => lang == searchLanguage)
        .map { case (lang, tags) =>
          api.ArticleTag(tags.getNMostFrequent(n), lang)
        }.toSeq)
    }

    val getTagUsageMap = MemoizeAutoRenew(() => {
      articleRepository.allTags.map(languageTags => languageTags.language -> new MostFrequentOccurencesList(languageTags.tags)).toMap
    })

    private[service] def addUrlOnResource(content: String): String = {
      val doc = stringToJsoupDocument(content)

      val embedTags = doc.select(s"$resourceHtmlEmbedTag").asScala.toList
      embedTags.foreach(addUrlOnEmbedTag)
      jsoupDocumentToString(doc)
    }

    private def addUrlOnEmbedTag(embedTag: Element) = {
      val resourceIdAttrName = Attributes.DataResource_Id.toString
      embedTag.hasAttr(resourceIdAttrName) match {
        case false =>
        case true => {
          val (resourceType, id) = (embedTag.attr(s"${Attributes.DataResource}"), embedTag.attr(resourceIdAttrName))
          embedTag.attr(s"${Attributes.DataUrl}", s"${externalApiUrls(resourceType)}/$id")
        }
      }
    }

    class MostFrequentOccurencesList(list: Seq[String]) {
      // Create a map where the key is a list entry, and the value is the number of occurences of this entry in the list
      private[this] val listToNumOccurencesMap: Map[String, Int] = list.groupBy(identity).mapValues(_.size)
      // Create an inverse of the map 'listToNumOccurencesMap': the key is number of occurences, and the value is a list of all entries that occured that many times
      private[this] val numOccurencesToListMap: Map[Int, Set[String]] = listToNumOccurencesMap.groupBy(x => x._2).mapValues(_.keySet)
      // Build a list sorted by the most frequent words to the least frequent words
      private[this] val mostFrequentOccorencesDec = numOccurencesToListMap.keys.toSeq.sorted
        .foldRight(Seq[String]())((current, result) => result ++ numOccurencesToListMap(current))

      def getNMostFrequent(n: Int): Seq[String] = mostFrequentOccorencesDec.slice(0, n)
    }

    def conceptWithId(id: Long, language: String): Option[api.Concept] =
      conceptRepository.withId(id).map(concept => converterService.toApiConcept(concept, language))

    def getContentByExternalId(externalId: String): Option[Content] =
      articleRepository.withExternalId(externalId) orElse conceptRepository.withExternalId(externalId)
  }
}
