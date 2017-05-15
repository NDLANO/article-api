/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import no.ndla.articleapi.model.api
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.ArticleApiProperties.{externalApiUrls, resourceHtmlEmbedTag}
import no.ndla.articleapi.caching.{Memoize, Memoize1}
import no.ndla.articleapi.integration.ConverterModule.{jsoupDocumentToString, stringToJsoupDocument}
import no.ndla.articleapi.service.converters.Attributes
import org.jsoup.nodes.Element

import scala.collection.JavaConverters._

trait ReadService {
  this: ArticleRepository with ConverterService =>
  val readService: ReadService

  class ReadService {
    def withId(id: Long): Option[api.Article] =
      articleRepository.withId(id)
        .map(addUrlsAndIdsOnEmbedResources)
        .map(converterService.toApiArticle)

    private[service] def addUrlsAndIdsOnEmbedResources(article: Article): Article = {
      val articleWithUrls = article.content.map(addIdAndUrlOnResource)
      article.copy(content = articleWithUrls)
    }

    def getNMostUsedTags(n: Int): Seq[api.ArticleTag] = {
      getTagUsageMap().map { case (lang, tags) =>
          api.ArticleTag(tags.getNMostFrequent(n), lang)
      }.toSeq
    }

    val getTagUsageMap = Memoize(() => {
      articleRepository.allTags.map(languageTags => languageTags.language -> new MostFrequentOccurencesList(languageTags.tags)).toMap
    })

    private[service] def addIdAndUrlOnResource(content: ArticleContent): ArticleContent = {
      val doc = stringToJsoupDocument(content.content)

      val embedTags = doc.select(s"$resourceHtmlEmbedTag").asScala.toList
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

  }
}
