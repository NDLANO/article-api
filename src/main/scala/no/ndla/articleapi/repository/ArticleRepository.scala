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
import no.ndla.articleapi.model.api.OptimisticLockException
import no.ndla.articleapi.model.domain.{Article, ArticleIds, ArticleTag}
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization.write
import org.postgresql.util.PGobject
import scalikejdbc._

import scala.util.{Failure, Success, Try}

trait ArticleRepository {
  this: DataSource =>
  val articleRepository: ArticleRepository

  class ArticleRepository extends LazyLogging with Repository[Article] {
    implicit val formats = org.json4s.DefaultFormats + Article.JSonSerializer

    def newArticle(article: Article)(implicit session: DBSession = AutoSession): Article = {
      val startRevision = 1
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(article))

      val articleId: Long = sql"insert into ${Article.table} (document, revision) values (${dataObject}, $startRevision)".updateAndReturnGeneratedKey().apply

      logger.info(s"Inserted new article: $articleId")
      article.copy(id=Some(articleId), revision=Some(startRevision))
    }

    def updateArticle(article: Article)(implicit session: DBSession = AutoSession): Try[Article] = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(article))

      val newRevision = article.revision.getOrElse(0) + 1
      val count = sql"update ${Article.table} set document=${dataObject}, revision=$newRevision where id=${article.id} and revision=${article.revision}".update.apply

      if (count != 1) {
        val message = s"Found revision mismatch when attempting to update article ${article.id}"
        logger.info(message)
        Failure(new OptimisticLockException)
      } else {
        logger.info(s"Updated article ${article.id}")
        Success(article.copy(revision=Some(newRevision)))
      }
    }

    def updateArticleFromDraftApi(article: Article)(implicit session: DBSession = AutoSession): Try[Article] = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(article))

      Try {
        sql"update ${Article.table} set document=${dataObject}, revision=${article.revision} where id=${article.id}".update.apply
      } match {
        case Success(count) if count == 1 =>
          logger.info(s"Updated article ${article.id}")
          Success(article)
        case Success(_) =>
          val message = s"Found revision mismatch when attempting to update article ${article.id}"
          logger.info(message)
          Failure(new OptimisticLockException)
        case Failure(ex) => Failure(ex)
      }
    }

    def insertWithExternalIds(article: Article, externalId: String, externalSubjectId: Seq[String])(implicit session: DBSession = AutoSession): Article = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(article))

      val articleId: Long = sql"insert into ${Article.table} (external_id, external_subject_id, document) values (${externalId}, ARRAY[${externalSubjectId}]::text[], ${dataObject})".updateAndReturnGeneratedKey().apply

      logger.info(s"Inserted article $externalId: $articleId")
      article.copy(id=Some(articleId))
    }

    def updateWithExternalId(article: Article, externalId: String)(implicit session: DBSession = AutoSession): Try[Article] = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(article))

      val expectedArticleRevision = 1
      Try(sql"update ${Article.table} set document=${dataObject} where external_id=${externalId} and revision=$expectedArticleRevision".updateAndReturnGeneratedKey().apply) match {
        case Success(articleId) =>
          logger.info(s"Updated article with external_id=$externalId, id=$articleId")
          Success(article.copy(id=Some(articleId)))
        case Failure(_) =>
          val message = "The revision stored in the database is newer than the one being updated. Please use the latest version from database when updating."

          logger.info(message)
          Failure(new OptimisticLockException(message))
      }
    }

    def allocateArticleId()(implicit session: DBSession = AutoSession): Long = {
      val startRevision = 0

      val articleId: Long = sql"insert into ${Article.table} (revision) values ($startRevision)".updateAndReturnGeneratedKey().apply
      logger.info(s"Allocated id for article $articleId")
      articleId
    }

    def allocateArticleIdWithExternal(externalId: String, externalSubjectId: Set[String])(implicit session: DBSession = AutoSession): Long = {
      val startRevision = 0

      val articleId: Long = sql"insert into ${Article.table} (external_id, external_subject_id, revision) values (${externalId}, ARRAY[${externalSubjectId}]::text[], $startRevision)".updateAndReturnGeneratedKey().apply

      logger.info(s"Allocated id for article $articleId (external id $externalId)")
      articleId
    }

    def updateWithExternalIdOverrideManualChanges(article: Article, externalId: String)(implicit session: DBSession = AutoSession): Try[Article] = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(article))

      val startRevision = 1
      Try(sql"update ${Article.table} set document=${dataObject}, revision=$startRevision where external_id=${externalId}".updateAndReturnGeneratedKey().apply) match {
        case Success(articleId) =>
          logger.info(s"Updated article with external_id=$externalId, id=$articleId. Revision reset to 1")
          Success(article.copy(id=Some(articleId)))
        case Failure(ex) =>
          logger.warn(s"Failed to update article with external id $externalId: ${ex.getMessage}")
          Failure(ex)
      }
    }

    def delete(articleId: Long)(implicit session: DBSession = AutoSession) = sql"delete from ${Article.table} where id = $articleId".update().apply

    def withId(articleId: Long): Option[Article] = articleWhere(sqls"ar.id=${articleId.toInt}")

    def withExternalId(externalId: String): Option[Article] = articleWhere(sqls"ar.external_id=$externalId")

    def exists(externalId: String): Boolean = getIdFromExternalId(externalId).isDefined

    def getIdFromExternalId(externalId: String)(implicit session: DBSession = AutoSession): Option[Long] = {
      sql"select id from ${Article.table} where external_id=${externalId}".map(rs => rs.long("id")).single.apply()
    }

    def getExternalIdFromId(id: Long)(implicit session: DBSession = AutoSession): Option[String] = {
      sql"select external_id from ${Article.table} where id=${id.toInt}".map(rs => rs.string("external_id")).single.apply()
    }

    def getAllIds(implicit session: DBSession = AutoSession): Seq[ArticleIds] = {
      sql"select id, external_id from ${Article.table}".map(rs => ArticleIds(rs.long("id"), rs.stringOpt("external_id"))).list.apply
    }

    def articleCount(implicit session: DBSession = AutoSession): Long = {
      sql"select count(*) from ${Article.table} where document is not NULL".map(rs => rs.long("count")).single().apply().getOrElse(0)
    }

    def getArticlesByPage(pageSize: Int, offset: Int)(implicit session: DBSession = AutoSession): Seq[Article] = {
      val ar = Article.syntax("ar")
      sql"select ${ar.result.*} from ${Article.as(ar)} where document is not NULL offset $offset limit $pageSize".map(Article(ar)).list.apply()
    }

    def allTags(implicit session: DBSession = AutoSession): Seq[ArticleTag] = {
      val allTags = sql"""select document->>'tags' from ${Article.table} where document is not NULL""".map(rs => rs.string(1)).list.apply

      allTags.flatMap(tag => parse(tag).extract[List[ArticleTag]]).groupBy(_.language)
        .map { case (language, tags) =>
          ArticleTag(tags.flatMap(_.tags), language)
        }.toList
    }

    override def minMaxId(implicit session: DBSession = AutoSession): (Long, Long) = {
      sql"select coalesce(MIN(id),0) as mi, coalesce(MAX(id),0) as ma from ${Article.table}".map(rs => {
        (rs.long("mi"), rs.long("ma"))
      }).single().apply() match {
        case Some(minmax) => minmax
        case None => (0L, 0L)
      }
    }

    def all(implicit session: DBSession = AutoSession): List[Article] = {
      val ar = Article.syntax("ar")
      sql"select ${ar.result.*} from ${Article.as(ar)} where document is not NULL".map(Article(ar)).list.apply()
    }

    def allWithExternalSubjectId(externalSubjectId: String): Seq[Article] = articlesWhere(sqls"$externalSubjectId=ANY(ar.external_subject_id)")

    override def documentsWithIdBetween(min: Long, max: Long): List[Article] = articlesWhere(sqls"ar.id between $min and $max").toList

    private def articleWhere(whereClause: SQLSyntax)(implicit session: DBSession = ReadOnlyAutoSession): Option[Article] = {
      val ar = Article.syntax("ar")
      sql"select ${ar.result.*} from ${Article.as(ar)} where ar.document is not NULL and $whereClause".map(Article(ar)).single.apply()
    }

    private def articlesWhere(whereClause: SQLSyntax)(implicit session: DBSession = ReadOnlyAutoSession): Seq[Article] = {
      val ar = Article.syntax("ar")
      sql"select ${ar.result.*} from ${Article.as(ar)} where ar.document is not NULL and $whereClause".map(Article(ar)).list.apply()
    }

  }
}
