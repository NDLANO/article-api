/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.repository

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.ArticleApiProperties
import no.ndla.articleapi.integration.DataSourceComponent
import no.ndla.articleapi.model.{Article, ArticleSummary}
import org.postgresql.util.PGobject
import scalikejdbc.{ConnectionPool, DB, DataSourceConnectionPool, _}
import no.ndla.network.ApplicationUrl

trait ArticleRepositoryComponent {
  this: DataSourceComponent =>
  val articleRepository: ArticleRepository

  class ArticleRepository extends LazyLogging {

    ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))

    def insert(article: Article, externalId: String)(implicit session: DBSession = AutoSession): Long = {
      import org.json4s.native.Serialization.write
      implicit val formats = org.json4s.DefaultFormats

      val json = write(article)
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(json)

      val articleId: Long = sql"insert into contentdata(external_id, document) values(${externalId}, ${dataObject})".updateAndReturnGeneratedKey().apply

      logger.info(s"Inserted node ${externalId}: $articleId")
      articleId
    }

    def update(article: Article, externalId: String)(implicit session: DBSession = AutoSession): Long = {
      import org.json4s.native.Serialization.write
      implicit val formats = org.json4s.DefaultFormats

      val json = write(article)
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(json)

      val articleId: Long = sql"update contentdata set document = ${dataObject} where external_id = ${externalId}".updateAndReturnGeneratedKey().apply

      logger.info(s"Updated node ${externalId}: $articleId")
      articleId
    }

    def getIdFromExternalId(externalId: String)(implicit session: DBSession = AutoSession): Option[Long] =
      sql"select id from contentdata where external_id=${externalId}".map(rs => rs.long("id")).single.apply()

    def minMaxId: (Long, Long) = {
      DB readOnly { implicit session =>
        sql"select min(id) as mi, max(id) as ma from contentdata;".map(rs => {
          (rs.long("mi"), rs.long("ma"))
        }).single().apply() match {
          case Some(minmax) => minmax
          case None => (0L, 0L)
        }
      }
    }

    def applyToAll(func: (List[Article]) => Unit): Unit = {
      val (minId, maxId) = minMaxId
      val groupRanges = Seq.range(minId, maxId + 1).grouped(ArticleApiProperties.IndexBulkSize).map(group => (group.head, group.last))

      DB readOnly { implicit session =>
        groupRanges.foreach(range => {
          func(
            sql"select id,document from contentdata where id between ${range._1} and ${range._2}".map(rs => {
              asArticle(rs.long("id").toString, rs.string("document"))
            }).toList.apply
          )
        })
      }
    }

    def all: List[Article] = {
      DB readOnly { implicit session =>
        sql"select id, document from contentdata".map(rs => asArticle(rs.string("id"), rs.string("document"))).list().apply()
      }
    }

    def withId(articleId: String): Option[Article] = {
      DB readOnly { implicit session =>
        sql"select document from contentdata where id = ${articleId.toInt}".map(rs => rs.string("document")).single.apply match {
          case Some(json) => Option(asArticle(articleId, json))
          case None => None
        }
      }
    }

    def withExternalId(externalId: String)(implicit session: DBSession = AutoSession): Option[ArticleSummary] =
        sql"select id, document from contentdata where external_id=$externalId".map(rs => asArticleSummary(rs.long("id"), rs.string("document"))).single.apply()

    def exists(externalId: String): Boolean = {
      DB readOnly { implicit session =>
        sql"select exists(select 1 from contentdata where external_id=${externalId})".map(rs => (rs.boolean(1))).single.apply match {
          case Some(t) => t
          case None => false
        }
      }
    }

    def asArticleSummary(articleId: Long, json: String): ArticleSummary = {
      import org.json4s.native.Serialization.read
      implicit val formats = org.json4s.DefaultFormats

      val meta = read[Article](json)
      ArticleSummary(articleId.toString, meta.title, ApplicationUrl.get + articleId, meta.copyright.license.license)
    }

    def asArticle(articleId: String, json: String): Article = {
      import org.json4s.native.Serialization.read
      implicit val formats = org.json4s.DefaultFormats

      val meta = read[Article](json)
      Article(
        articleId,
        meta.title,
        meta.content,
        meta.copyright,
        meta.tags,
        meta.requiredLibraries,
        meta.visualElement,
        meta.introduction,
        meta.created,
        meta.updated,
        meta.contentType)
    }
  }
}
