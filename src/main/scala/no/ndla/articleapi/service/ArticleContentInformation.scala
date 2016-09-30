/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service

import no.ndla.articleapi.model.{Article, ArticleSummary}
import no.ndla.articleapi.repository.ArticleRepositoryComponent
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

import scala.collection.JavaConversions._
import scala.annotation.tailrec

trait ArticleContentInformation {
  this: ArticleRepositoryComponent =>

  object ArticleContentInformation {
    def getHtmlTagsMap: Map[String, Seq[String]] = {
      @tailrec def getHtmlTagsMap(nodes: Seq[Article], tagsMap: Map[String, List[String]]): Map[String, List[String]] = {
        if (nodes.isEmpty)
          return tagsMap

        val node = nodes.head

        val tagMaps = node.content.map(article => {
          val elements = Jsoup.parseBodyFragment(article.content).select("article").select("*").toList
          buildMap(node.id, elements)
        }).foldLeft(tagsMap)((map, articleMap) => addOrUpdateMap(map, articleMap))

        getHtmlTagsMap(nodes.tail, tagMaps)
      }

      getHtmlTagsMap(articleRepository.all, Map())
    }

    private def buildMap(id: String, elements: List[Element]): Map[String, List[String]] =
      elements.foldLeft(Map[String, List[String]]())((map, element) => {
        val list = map.getOrElse(element.tagName, List())
        map + (element.tagName -> (list ++ Seq(id)))
      })

    private def addOrUpdateMap(mapToUpdate: Map[String, Seq[String]], mapToInsert: Map[String, List[String]]): Map[String, List[String]] = {
      val mergedLists = mapToUpdate.toList ++ mapToInsert.toList
      val a = mergedLists.groupBy(_._1)

      a.map { case (k, v) => k -> v.unzip._2.flatten.distinct }
    }

    def getExternalEmbedResources: Map[String, Seq[String]] = {
      articleRepository.all.flatMap(articleInfo => {
        val externalId = articleRepository.getExternalIdFromId(articleInfo.id.toInt).getOrElse("unknown ID")
        val urls = articleInfo.content.flatMap(content => {
          val elements = Jsoup.parseBodyFragment(content.content).select("""figure[data-resource=external]""")
          elements.toList.map(el => el.attr("data-url"))
        })

        urls.isEmpty match {
          case true => None
          case false => Some(externalId -> urls.distinct)
        }
      }).toMap
    }

  }
}

