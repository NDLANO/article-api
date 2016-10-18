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
import no.ndla.articleapi.model.domain.Article
import org.postgresql.util.PGobject
import scalikejdbc.{ConnectionPool, DB, DataSourceConnectionPool, _}
import org.json4s.native.Serialization.write

trait ArticleRepositoryComponent {
  this: DataSourceComponent =>
  val articleRepository: ArticleRepository

  class ArticleRepository extends LazyLogging {
    implicit val formats = org.json4s.DefaultFormats + Article.JSonSerializer
    ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))

    def insert(article: Article, externalId: String, externalSubjectId: Seq[String])(implicit session: DBSession = AutoSession): Long = {
      val c = Article.column
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(article))

      val articleId: Long = sql"insert into ${Article.table} (external_id, external_subject_id, document) values (${externalId}, ARRAY[${externalSubjectId}], ${dataObject})".updateAndReturnGeneratedKey().apply

      logger.info(s"Inserted node ${externalId}: $articleId")
      articleId
    }

    def update(article: Article, externalId: String)(implicit session: DBSession = AutoSession): Long = {
      val c = Article.column
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(article))

      val articleId: Long = sql"update ${Article.table} set document=${dataObject} where external_id=${externalId}".updateAndReturnGeneratedKey().apply

      logger.info(s"Updated node $externalId: $articleId")
      articleId
    }

    def getIdFromExternalId(externalId: String)(implicit session: DBSession = AutoSession): Option[Long] = {
      val ar = Article.syntax("ar")
      sql"select ${ar.result.id} from ${Article.as(ar)} where external_id=${externalId}"
        .map(Article(ar)).single.apply().flatMap(_.id)
    }

    def getExternalIdFromId(id: Long)(implicit session: DBSession = AutoSession): Option[String] = {
      val ar = Article.syntax("ar")
      sql"select ar.external_id from ${Article.as(ar)} where id=${id.toInt}"
      .map(rs => rs.string("external_id")).single.apply()
    }

    private def minMaxId(implicit session: DBSession = AutoSession): (Long, Long) = {
      sql"select min(id) as mi, max(id) as ma from ${Article.table}".map(rs => {
        (rs.long("mi"), rs.long("ma"))
      }).single().apply() match {
        case Some(minmax) => minmax
        case None => (0L, 0L)
      }
    }

    def applyToAll(func: (List[Article]) => Unit)(implicit session: DBSession = AutoSession): Unit = {
      val (minId, maxId) = minMaxId
      val groupRanges = Seq.range(minId, maxId + 1).grouped(ArticleApiProperties.IndexBulkSize).map(group => (group.head, group.last))
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

    def withId(articleId: Long): Option[Article] =
      articleWhere(sqls"ar.id=${articleId.toInt}")

    def exists(externalId: String): Boolean =
      articleWhere(sqls"ar.external_id=$externalId").isDefined

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
