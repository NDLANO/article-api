/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service

import no.ndla.articleapi.ArticleApiProperties.TopicAPIUrl
import no.ndla.articleapi.integration.MappingApiClient
import no.ndla.articleapi.model.ArticleTag

import scala.io.Source
import scala.util.matching.Regex

trait TagsService {
  this: MappingApiClient =>
  val tagsService: TagsService

  val pattern = new Regex("http:\\/\\/psi\\..*\\/#(.+)")

  class TagsService {
    def forContent(nid: String): List[ArticleTag] = {
      import org.json4s.native.JsonMethods._
      import org.json4s.native.Serialization.read
      implicit val formats = org.json4s.DefaultFormats

      val jsonString = Source.fromURL(TopicAPIUrl + nid).mkString
      val json = parse(jsonString)

      read[Keywords](jsonString)
        .keyword
        .flatMap(_.names)
        .flatMap(_.data)
        .flatMap(_.toIterable)
        .map(t => (getISO639(t._1), t._2.trim.toLowerCase))
        .groupBy(_._1).map(entry => (entry._1, entry._2.map(_._2)))
        .map(t => ArticleTag(t._2, t._1)).toList
    }

    def getISO639(languageUrl:String): Option[String] = {
      Option(languageUrl) collect { case pattern(group) => group } match {
        case Some(x) => if (x == "language-neutral") None else mappingApiClient.get6391CodeFor6392Code(x)
        case None => None
      }
    }
  }

}

case class Keywords(keyword: List[Keyword])
case class Keyword(psi: Option[String], topicId: Option[String], visibility: Option[String], approved: Option[String], processState: Option[String], psis: List[String],
                   originatingSites: List[String], types: List[Any], names: List[KeywordName])

case class Type(typeId:String)
case class TypeName(isoLanguageCode: String)

case class KeywordName(wordclass: String, data: List[Map[String,String]])
case class KeywordNameName(isoLanguageCode: String)
