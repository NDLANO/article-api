/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.repository

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.integration.DataSource
import no.ndla.articleapi.model.api.NotFoundException
import no.ndla.articleapi.model.domain.{Article, ArticleIds, ArticleTag}
import org.json4s.Formats
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization.write
import org.postgresql.util.PGobject
import scalikejdbc._

import scala.util.{Failure, Success, Try}

trait ArticleRepository {
  this: DataSource =>
  val articleRepository: ArticleRepository

  class ArticleRepository extends LazyLogging with Repository[Article] {
    implicit val formats: Formats = org.json4s.DefaultFormats + Article.JSonSerializer

    def updateArticleFromDraftApi(article: Article, externalIds: List[String])(
        implicit session: DBSession = AutoSession): Try[Article] = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(article))

      Try {
        sql"""update ${Article.table}
              set document=$dataObject,
                  external_id=ARRAY[$externalIds]::text[],
                  revision=${article.revision}
              where id=${article.id}
          """.update.apply
      } match {
        case Success(count) if count == 1 =>
          logger.info(s"Updated article ${article.id}")
          Success(article)
        case Success(_) =>
          logger.info(s"No article with id ${article.id} exists, creating...")
          Try {
            sql"""
                  insert into ${Article.table} (id, document, external_id, revision)
                  values (${article.id}, $dataObject, ARRAY[$externalIds]::text[], ${article.revision})
              """.updateAndReturnGeneratedKey().apply
          }.map(_ => article)

        case Failure(ex) => Failure(ex)
      }
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
        // Article with id $articleId does not exist.
        Success(articleId)
      }
    }

    def withId(articleId: Long): Option[Article] = articleWhere(sqls"ar.id=${articleId.toInt}")

    def withExternalId(externalId: String): Option[Article] = articleWhere(sqls"$externalId = any (ar.external_id)")

    def getIdFromExternalId(externalId: String)(implicit session: DBSession = AutoSession): Option[Long] = {
      sql"select id from ${Article.table} where $externalId = any(external_id)"
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
        .map(Article.fromResultSet(ar))
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

    def getTags(input: String, pageSize: Int, offset: Int, language: String)(
        implicit session: DBSession = AutoSession): (Seq[String], Int) = {
      val sanitizedInput = input.replaceAll("%", "")
      val sanitizedLanguage = language.replaceAll("%", "")
      val langOrAll = if (sanitizedLanguage == "all" || sanitizedLanguage == "") "%" else sanitizedLanguage

      val tags = sql"""select tags from 
              (select distinct JSONB_ARRAY_ELEMENTS_TEXT(tagObj->'tags') tags from
              (select JSONB_ARRAY_ELEMENTS(document#>'{tags}') tagObj from ${Article.table}) _
              where tagObj->>'language' like ${langOrAll}
              order by tags) sorted_tags
              where sorted_tags.tags like ${sanitizedInput + '%'}
              offset ${offset}
              limit ${pageSize}
                      """
        .map(rs => rs.string("tags"))
        .toList()
        .apply

      val tagsCount =
        sql"""
              select count(*) from 
              (select distinct JSONB_ARRAY_ELEMENTS_TEXT(tagObj->'tags') tags from
              (select JSONB_ARRAY_ELEMENTS(document#>'{tags}') tagObj from ${Article.table}) _
              where tagObj->>'language' like  ${langOrAll}) all_tags
              where all_tags.tags like ${sanitizedInput + '%'};
           """
          .map(rs => rs.int("count"))
          .single()
          .apply()
          .getOrElse(0)

      (tags, tagsCount)

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
        .map(Article.fromResultSet(ar))
        .single
        .apply()
    }

    private def articlesWhere(whereClause: SQLSyntax)(
        implicit session: DBSession = ReadOnlyAutoSession): Seq[Article] = {
      val ar = Article.syntax("ar")
      sql"select ${ar.result.*} from ${Article.as(ar)} where ar.document is not NULL and $whereClause"
        .map(Article.fromResultSet(ar))
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
