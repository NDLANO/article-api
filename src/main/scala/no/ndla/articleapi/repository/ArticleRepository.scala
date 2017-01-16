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
import no.ndla.articleapi.integration.DataSource
import no.ndla.articleapi.model.domain.Article
import org.postgresql.util.PGobject
import scalikejdbc.{ConnectionPool, DB, DataSourceConnectionPool, _}
import org.json4s.native.Serialization.write

trait ArticleRepository {
  this: DataSource =>
  val articleRepository: ArticleRepository

  class ArticleRepository extends LazyLogging {
    implicit val formats = org.json4s.DefaultFormats + Article.JSonSerializer
    ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))

    def insert(article: Article)(implicit session: DBSession = AutoSession) = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(article))

      val articleId: Long = sql"insert into ${Article.table} (document) values (${dataObject})".updateAndReturnGeneratedKey().apply

      logger.info(s"Inserted new article: $articleId")
      article.copy(id=Some(articleId))
    }

    def insertWithExternalIds(article: Article, externalId: String, externalSubjectId: Seq[String])(implicit session: DBSession = AutoSession): Long = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(article))

      val articleId: Long = sql"insert into ${Article.table} (external_id, external_subject_id, document) values (${externalId}, ARRAY[${externalSubjectId}], ${dataObject})".updateAndReturnGeneratedKey().apply

      logger.info(s"Inserted node $externalId: $articleId")
      articleId
    }

    def update(article: Article, externalId: String)(implicit session: DBSession = AutoSession): Long = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(article))

      val articleId: Long = sql"update ${Article.table} set document=${dataObject} where external_id=${externalId}".updateAndReturnGeneratedKey().apply

      logger.info(s"Updated node $externalId: $articleId")
      articleId
    }

    def withId(articleId: Long): Option[Article] =
      articleWhere(sqls"ar.id=${articleId.toInt}")

    def exists(externalId: String): Boolean =
      articleWhere(sqls"ar.external_id=$externalId").isDefined

    def getIdFromExternalId(externalId: String)(implicit session: DBSession = AutoSession): Option[Long] = {
      sql"select id from ${Article.table} where external_id=${externalId}"
        .map(rs => rs.long("id")).single.apply()
    }

    def getExternalIdFromId(id: Long)(implicit session: DBSession = AutoSession): Option[String] = {
      sql"select external_id from ${Article.table} where id=${id.toInt}"
        .map(rs => rs.string("external_id")).single.apply()
    }

    private def minMaxId(implicit session: DBSession = AutoSession): (Long, Long) = {
      sql"select coalesce(MIN(id),0) as mi, coalesce(MAX(id),0) as ma from ${Article.table}".map(rs => {
        (rs.long("mi"), rs.long("ma"))
      }).single().apply() match {
        case Some(minmax) => minmax
        case None => (0L, 0L)
      }
    }

    def applyToAll(func: (List[Article]) => Unit)(implicit session: DBSession = AutoSession): Unit = {
      val (minId, maxId) = minMaxId
      val groupRanges = Seq.range(minId, maxId).grouped(ArticleApiProperties.IndexBulkSize).map(group => (group.head, group.last + 1))
      val ar = Article.syntax("ar")

      groupRanges.foreach(range => {
        func(
          sql"select ${ar.result.*} from ${Article.as(ar)} where ${ar.id} between ${range._1} and ${range._2}".map(Article(ar)).toList.apply
        )
      })
    }

    def all(implicit session: DBSession = AutoSession): List[Article] = {
      val ar = Article.syntax("ar")
      sql"select ${ar.result.*} from ${Article.as(ar)}".map(Article(ar)).list.apply()
    }

    def allWithExternalSubjectId(externalSubjectId: String): Seq[Article] =
      articlesWhere(sqls"$externalSubjectId=ANY(ar.external_subject_id)")

    private def articleWhere(whereClause: SQLSyntax)(implicit session: DBSession = ReadOnlyAutoSession): Option[Article] = {
      val ar = Article.syntax("ar")
      sql"select ${ar.result.*} from ${Article.as(ar)} where $whereClause".map(Article(ar)).single.apply()
    }

    private def articlesWhere(whereClause: SQLSyntax)(implicit session: DBSession = ReadOnlyAutoSession): Seq[Article] = {
      val ar = Article.syntax("ar")
      sql"select ${ar.result.*} from ${Article.as(ar)} where $whereClause".map(Article(ar)).list.apply()
    }

  }
}
