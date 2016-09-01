/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service

import no.ndla.articleapi.model.{ArticleInformation, ArticleSummary}
import no.ndla.articleapi.repository.ArticleRepositoryComponent
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

import scala.collection.JavaConversions._
import scala.annotation.tailrec

trait HtmlTagsUsage {
  this: ArticleRepositoryComponent =>

  object HtmlTagsUsage {
    def getHtmlTagsMap: Map[String, Seq[String]] = {
      @tailrec def getHtmlTagsMap(nodes: Seq[ArticleInformation], tagsMap: Map[String, List[String]]): Map[String, List[String]] = {
        if (nodes.isEmpty)
          return tagsMap

        val node = nodes.head

        val tagMaps = node.article.map(article => {
          val elements = Jsoup.parseBodyFragment(article.article).select("article").select("*").toList
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
  }
}

