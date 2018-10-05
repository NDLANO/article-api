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
import no.ndla.articleapi.model.api.NotFoundException
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

    def updateArticleFromDraftApi(article: Article, externalIds: List[String])(
        implicit session: DBSession = AutoSession): Try[Article] = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(article))

      Try {
        sql"update ${Article.table} set document=${dataObject}, external_id=ARRAY[${externalIds}]::text[], revision=${article.revision} where id=${article.id}".update.apply
      } match {
        case Success(count) if count == 1 =>
          logger.info(s"Updated article ${article.id}")
          Success(article)
        case Success(_) =>
          logger.error(s"No article with id ${article.id} exists, recreating...")
          Try {
            sql"""
                  insert into ${Article.table} (id, document, external_id, revision)
                  values (${article.id}, $dataObject, ARRAY[$externalIds]::text[], ${article.revision})
              """.updateAndReturnGeneratedKey().apply
          } match {
            case Success(_)  => Success(article)
            case Failure(ex) => Failure(ex)
          }
        case Failure(ex) => Failure(ex)
      }
    }

    def allocateArticleId()(implicit session: DBSession = AutoSession): Long = {
      val startRevision = 0

      val articleId: Long =
        sql"insert into ${Article.table} (revision) values ($startRevision)".updateAndReturnGeneratedKey().apply
      logger.info(s"Allocated id for article $articleId")
      articleId
    }

    def allocateArticleIdWithExternalIds(externalIds: List[String], externalSubjectId: Set[String])(
        implicit session: DBSession = AutoSession): Long = {
      val startRevision = 0

      val articleId: Long =
        sql"""
             insert into ${Article.table} (external_id, external_subject_id, revision)
             values (ARRAY[${externalIds}]::text[], ARRAY[${externalSubjectId}]::text[], $startRevision)
          """
          .updateAndReturnGeneratedKey()
          .apply

      logger.info(s"Allocated id for article $articleId (external ids ${externalIds.mkString(",")})")
      articleId
    }

    def unpublish(articleId: Long)(implicit session: DBSession = AutoSession): Try[Long] = {
      val numRows = sql"update ${Article.table} set document=null where id=$articleId".update().apply
      if (numRows == 1) {
        Success(articleId)
      } else {
        Failure(NotFoundException(s"Article with id $articleId does not exist"))
      }
    }

    def delete(articleId: Long)(implicit session: DBSession = AutoSession): Try[Long] = {
      val numRows = sql"delete from ${Article.table} where id = $articleId".update().apply
      if (numRows == 1) {
        Success(articleId)
      } else {
        Failure(NotFoundException(s"Article with id $articleId does not exist"))
      }
    }

    def withId(articleId: Long): Option[Article] = articleWhere(sqls"ar.id=${articleId.toInt}")

    def withExternalId(externalId: String): Option[Article] = articleWhere(sqls"$externalId = any (ar.external_id)")

    def getIdFromExternalId(externalId: String)(implicit session: DBSession = AutoSession): Option[Long] = {
      sql"select id from ${Article.table} where ${externalId} = any(external_id)"
        .map(rs => rs.long("id"))
        .single
        .apply()
    }

    private def externalIdsFromResultSet(wrappedResultSet: WrappedResultSet): List[String] = {
      Option(wrappedResultSet.array("external_id"))
        .map(_.getArray.asInstanceOf[Array[String]])
        .getOrElse(Array.empty)
        .toList
    }

    def getExternalIdsFromId(id: Long)(implicit session: DBSession = AutoSession): List[String] = {
      sql"select external_id from ${Article.table} where id=${id.toInt}"
        .map(externalIdsFromResultSet)
        .single
        .apply
        .getOrElse(List.empty)
    }

    def getAllIds(implicit session: DBSession = AutoSession): Seq[ArticleIds] = {
      sql"select id, external_id from ${Article.table}"
        .map(
          rs =>
            ArticleIds(
              rs.long("id"),
              externalIdsFromResultSet(rs)
          ))
        .list
        .apply
    }

    def articleCount(implicit session: DBSession = AutoSession): Long = {
      sql"select count(*) from ${Article.table} where document is not NULL"
        .map(rs => rs.long("count"))
        .single()
        .apply()
        .getOrElse(0)
    }

    def getArticlesByPage(pageSize: Int, offset: Int)(implicit session: DBSession = AutoSession): Seq[Article] = {
      val ar = Article.syntax("ar")
      sql"select ${ar.result.*} from ${Article.as(ar)} where document is not NULL offset $offset limit $pageSize"
        .map(Article(ar))
        .list
        .apply()
    }

    def allTags(implicit session: DBSession = AutoSession): Seq[ArticleTag] = {
      val allTags = sql"""select document->>'tags' from ${Article.table} where document is not NULL"""
        .map(rs => rs.string(1))
        .list
        .apply

      allTags
        .flatMap(tag => parse(tag).extract[List[ArticleTag]])
        .groupBy(_.language)
        .map {
          case (language, tags) =>
            ArticleTag(tags.flatMap(_.tags), language)
        }
        .toList
    }

    override def minMaxId(implicit session: DBSession = AutoSession): (Long, Long) = {
      sql"select coalesce(MIN(id),0) as mi, coalesce(MAX(id),0) as ma from ${Article.table}"
        .map(rs => {
          (rs.long("mi"), rs.long("ma"))
        })
        .single()
        .apply() match {
        case Some(minmax) => minmax
        case None         => (0L, 0L)
      }
    }

    override def documentsWithIdBetween(min: Long, max: Long): List[Article] =
      articlesWhere(sqls"ar.id between $min and $max").toList

    private def articleWhere(whereClause: SQLSyntax)(
        implicit session: DBSession = ReadOnlyAutoSession): Option[Article] = {
      val ar = Article.syntax("ar")
      sql"select ${ar.result.*} from ${Article.as(ar)} where ar.document is not NULL and $whereClause"
        .map(Article(ar))
        .single
        .apply()
    }

    private def articlesWhere(whereClause: SQLSyntax)(
        implicit session: DBSession = ReadOnlyAutoSession): Seq[Article] = {
      val ar = Article.syntax("ar")
      sql"select ${ar.result.*} from ${Article.as(ar)} where ar.document is not NULL and $whereClause"
        .map(Article(ar))
        .list
        .apply()
    }

    def getArticleIdsFromExternalId(externalId: String)(
        implicit session: DBSession = ReadOnlyAutoSession): Option[ArticleIds] = {
      val ar = Article.syntax("ar")
      sql"select id, external_id from ${Article.as(ar)} where $externalId=ANY(ar.external_id)"
        .map(
          rs =>
            ArticleIds(
              rs.long("id"),
              externalIdsFromResultSet(rs)
          ))
        .single
        .apply
    }

  }

}
