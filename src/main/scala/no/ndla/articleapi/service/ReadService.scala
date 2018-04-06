/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import no.ndla.articleapi.ArticleApiProperties.externalApiUrls
import no.ndla.validation.EmbedTagRules.ResourceHtmlEmbedTag
import no.ndla.articleapi.caching.MemoizeAutoRenew
import no.ndla.validation.HtmlTagRules.{jsoupDocumentToString, stringToJsoupDocument}
import no.ndla.articleapi.model.api
import no.ndla.articleapi.model.api.NotFoundException
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.model.domain.Language._
import no.ndla.articleapi.repository.{ArticleRepository, ConceptRepository}
import no.ndla.validation.TagAttributes
import org.jsoup.nodes.Element

import scala.math.max
import scala.collection.JavaConverters._
import scala.util.{Failure, Try}

trait ReadService {
  this: ArticleRepository with ConceptRepository with ConverterService =>
  val readService: ReadService

  class ReadService {
    def getInternalIdByExternalId(externalId: Long): Option[api.ArticleIdV2] =
      articleRepository.getIdFromExternalId(externalId.toString).map(api.ArticleIdV2)

    def withIdV2(id: Long, language: String, fallback: Boolean = false): Try[api.ArticleV2] = {
      articleRepository.withId(id).map(addUrlsOnEmbedResources) match {
        case None => Failure(NotFoundException(s"The article with id $id was not found"))
        case Some(article) => converterService.toApiArticleV2(article, language, fallback)
      }
    }

    private[service] def addUrlsOnEmbedResources(article: Article): Article = {
      val articleWithUrls = article.content.map(content => content.copy(content = addUrlOnResource(content.content)))
      val visualElementWithUrls = article.visualElement.map(visual => visual.copy(resource = addUrlOnResource(visual.resource)))

      article.copy(content = articleWithUrls, visualElement = visualElementWithUrls)
    }

    def getNMostUsedTags(n: Int, language: String): Option[api.ArticleTag] = {
      val tagUsageMap = getTagUsageMap()
      val searchLanguage = getSearchLanguage(language, supportedLanguages)

      tagUsageMap.get(searchLanguage)
        .map(tags => api.ArticleTag(tags.getNMostFrequent(n), searchLanguage))
    }

    def getArticlesByPage(pageNo: Int, pageSize: Int, lang: String, fallback: Boolean = false): api.ArticleDump = {
      val (safePageNo, safePageSize) = (max(pageNo, 1), max(pageSize, 0))
      val results = articleRepository.getArticlesByPage(safePageSize, (safePageNo - 1) * safePageSize)
        .flatMap(article => converterService.toApiArticleV2(article, lang, fallback).toOption)

      api.ArticleDump(articleRepository.articleCount, pageNo, pageSize, lang, results)
    }

    def getArticleDomainDump(pageNo: Int, pageSize: Int): api.ArticleDomainDump = {
      val (safePageNo, safePageSize) = (max(pageNo, 1), max(pageSize, 0))
      val results = articleRepository.getArticlesByPage(safePageSize, (safePageNo - 1) * safePageSize)

      api.ArticleDomainDump(articleRepository.articleCount, pageNo, pageSize, results)
    }

    val getTagUsageMap = MemoizeAutoRenew(() => {
      articleRepository.allTags.map(languageTags => languageTags.language -> new MostFrequentOccurencesList(languageTags.tags)).toMap
    })

    private[service] def addUrlOnResource(content: String): String = {
      val doc = stringToJsoupDocument(content)

      val embedTags = doc.select(s"$ResourceHtmlEmbedTag").asScala.toList
      embedTags.foreach(addUrlOnEmbedTag)
      jsoupDocumentToString(doc)
    }

    private def addUrlOnEmbedTag(embedTag: Element) = {
      val resourceIdAttrName = TagAttributes.DataResource_Id.toString
      embedTag.hasAttr(resourceIdAttrName) match {
        case false =>
        case true =>
          val (resourceType, id) = (embedTag.attr(s"${TagAttributes.DataResource}"), embedTag.attr(resourceIdAttrName))
          embedTag.attr(s"${TagAttributes.DataUrl}", s"${externalApiUrls(resourceType)}/$id")
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

    def conceptWithId(id: Long, language: String, fallback: Boolean): Try[api.Concept] =
      conceptRepository.withId(id) match {
        case None => Failure(NotFoundException(s"The concept with id $id was not found"))
        case Some(concept) => converterService.toApiConcept(concept, language, fallback)
      }

    def getContentByExternalId(externalId: String): Option[Content] =
      articleRepository.withExternalId(externalId) orElse conceptRepository.withExternalId(externalId)

    def getArticleIdByExternalId(externalId: String): Option[Long] =
      articleRepository.getIdFromExternalId(externalId)

    def getConceptIdByExternalId(externalId: String): Option[Long] =
      conceptRepository.getIdFromExternalId(externalId)
  }

}
