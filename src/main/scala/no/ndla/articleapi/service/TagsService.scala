/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service

import scala.util.Try
import java.net.URL
import scala.io.Source

import scala.util.matching.Regex
import no.ndla.articleapi.ArticleApiProperties.TopicAPIUrl
import no.ndla.articleapi.model.domain.ArticleTag
import no.ndla.mapping.ISO639.get6391CodeFor6392Code

trait TagsService {
  val tagsService: TagsService

  val pattern = new Regex("http:\\/\\/psi\\..*\\/#(.+)")

  class TagsService {
    def forContent(nid: String): Try[List[ArticleTag]] = {
      import org.json4s.native.Serialization.read
      implicit val formats = org.json4s.DefaultFormats

      Try(new URL(TopicAPIUrl + nid).openStream).map(stream => {
       read[Keywords](Source.fromInputStream(stream).mkString)
         .keyword
         .flatMap(_.names)
         .flatMap(_.data)
         .flatMap(_.toIterable)
         .map(t => (getISO639(t._1), t._2.trim.toLowerCase))
         .groupBy(_._1).map(entry => (entry._1, entry._2.map(_._2)))
         .map(t => ArticleTag(t._2, t._1)).toList
     })
    }

    def getISO639(languageUrl:String): Option[String] = {
      Option(languageUrl) collect { case pattern(group) => group } match {
        case Some(x) => if (x == "language-neutral") None else get6391CodeFor6392Code(x)
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
