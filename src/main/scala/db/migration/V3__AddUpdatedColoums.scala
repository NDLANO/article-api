/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package db.migration

import java.sql.Connection

import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.postgresql.util.PGobject
import scalikejdbc._

class V3__AddUpdatedColoums extends JdbcMigration {

  implicit val formats = org.json4s.DefaultFormats

  override def migrate(connection: Connection) = {
    val db = DB(connection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      allArticles.map(convertArticleUpdate).foreach(update)
    }
  }

  def allArticles(implicit session: DBSession): List[V3_DBArticleMetaInformation] = {
    sql"select id, document from contentdata".map(rs => V3_DBArticleMetaInformation(rs.long("id"), rs.string("document"))).list().apply()
  }

  def convertArticleUpdate(articleMeta: V3_DBArticleMetaInformation) = {
    val oldDocument = parse(articleMeta.document)
    val updatedJson = parse(s"""{"updatedBy": "content-import-client"}""")

    val mergedDoc = oldDocument merge updatedJson

    articleMeta.copy(document = compact(render(mergedDoc)))
  }


  def update(articleMeta: V3_DBArticleMetaInformation)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(articleMeta.document)

    sql"update contentdata set document = $dataObject where id = ${articleMeta.id}".update().apply
  }

}

case class V3_DBArticleMetaInformation(id: Long, document: String)
