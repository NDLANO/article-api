/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service

import no.ndla.articleapi.model.domain.Article
import no.ndla.articleapi.repository.ArticleRepositoryComponent
import no.ndla.articleapi.ArticleApiProperties.resourceHtmlEmbedTag
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import scala.collection.JavaConversions._
import scala.annotation.tailrec

trait ArticleContentInformation {
  this: ArticleRepositoryComponent =>

  object ArticleContentInformation {
    def getHtmlTagsMap: Map[String, Seq[Long]] = {
      @tailrec def getHtmlTagsMap(nodes: Seq[Article], tagsMap: Map[String, List[Long]]): Map[String, List[Long]] = {
        if (nodes.isEmpty)
          return tagsMap

        val node = nodes.head

        val tagMaps = node.content.map(article => {
          val elements = Jsoup.parseBodyFragment(article.content).select("body").first.children.select("*").toList
          buildMap(node.id.get, elements)
        }).foldLeft(tagsMap)((map, articleMap) => mergeMaps(map, articleMap))

        getHtmlTagsMap(nodes.tail, tagMaps)
      }

      getHtmlTagsMap(articleRepository.all, Map())
    }

    private def buildMap(id: Long, elements: List[Element]): Map[String, List[Long]] =
      elements.foldLeft(Map[String, List[Long]]())((map, element) => {
        val list = map.getOrElse(element.tagName, List())
        map + (element.tagName -> (list ++ Seq(id)))
      })

    private def mergeMaps(mapToUpdate: Map[String, List[Long]], mapToInsert: Map[String, List[Long]]): Map[String, List[Long]] = {
      val mergedLists = mapToUpdate.toList ++ mapToInsert.toList
      mergedLists.groupBy(_._1).map { case (k, v) => k -> v.unzip._2.flatten.distinct }
    }

    def getExternalEmbedResources(subjectId: String): Map[String, Seq[String]] = {
      articleRepository.allWithExternalSubjectId(subjectId).flatMap(articleInfo => {
        val externalId = articleRepository.getExternalIdFromId(articleInfo.id.get).getOrElse("unknown ID")
        val urls = articleInfo.content.flatMap(content => {
          val elements = Jsoup.parseBodyFragment(content.content).select(s"""$resourceHtmlEmbedTag[data-resource~=(external|nrk)]""")
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

