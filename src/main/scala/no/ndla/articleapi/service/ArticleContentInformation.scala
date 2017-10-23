/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.ArticleApiProperties.resourceHtmlEmbedTag
import no.ndla.articleapi.integration.ConverterModule.stringToJsoupDocument
import no.ndla.articleapi.model.api.ArticleV2
import no.ndla.articleapi.model.domain.{Article, HtmlFaultRapport}
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.service.converters.Attributes
import no.ndla.articleapi.service.converters.ResourceType._
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.collection.immutable

trait ArticleContentInformation {
  this: ArticleRepository with ReadService =>

  object ArticleContentInformation extends LazyLogging {
    def getHtmlTagsMap: Map[String, Seq[Long]] = {
      @tailrec def getHtmlTagsMap(nodes: Seq[Article], tagsMap: Map[String, List[Long]]): Map[String, List[Long]] = {
        if (nodes.isEmpty)
          return tagsMap

        val node = nodes.head

        val tagMaps = node.content.map(article => {
          val elements = Jsoup.parseBodyFragment(article.content).select("body").first.children.select("*").asScala.toList
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
          val resourceTypes = Seq(ExternalContent, Kahoot, Prezi, Commoncraft, NdlaFilmIundervisning, NRKContent).mkString("|")
          val elements = Jsoup.parseBodyFragment(content.content).select(s"""$resourceHtmlEmbedTag[${Attributes.DataResource}~=($resourceTypes)]""")
          elements.asScala.toList.map(el => el.attr("data-url"))
        })

        urls.isEmpty match {
          case true => None
          case false => Some(externalId -> urls.distinct)
        }
      }).toMap
    }

    def getFaultyHtmlReport: String = {
      logger.info("Start FaultyHtmlReport: searching for header elements in Lists in all articles")
      val start = System.currentTimeMillis()
      var errorMessages: List[HtmlFaultRapport] = immutable.List()
      val ids = articleRepository.getAllIds

      logger.info(s"Found ${ids.length} article ids")
      ids.foreach(m => {
        val article = articleRepository.withId(m.articleId)
        article match {
          case Some(art) =>
            art.content.foreach(c => {
              val listElements = stringToJsoupDocument(c.content).select("li").asScala
              listElements.foreach(li => {
                val hTags = li.select("h1, h2, h3, h4, h5, h6").asScala
                hTags.foreach(h => {
                  val error = s"html element $h er ikke lov inni: [$li]"
                  errorMessages = HtmlFaultRapport(art.id.getOrElse(-1).toString, error) :: errorMessages
                })
              })
            })
          case None => logger.warn(s"Did not find article given id ${m.articleId} gotten from articleRepository.getAllIds, should be investigated if not due to race condition")
        }
      })
      val stop = System.currentTimeMillis()
      logger.info(s"Done searching for header elements in Lists time taken ${stop - start} ms. Found ${errorMessages.size} faults.")
      //Change the list to CSV format with header row.
      (s"""artikkel id;feil funnet""" :: errorMessages.map(e => s"""${e.articleId};"${e.faultMessage}"""")).mkString("\n")
    }

  }

}

