/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package db.migration

import java.sql.Connection

import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import org.json4s.JsonAST.{JArray, JField, JObject}
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization._
import org.postgresql.util.PGobject
import scalikejdbc._

class V5__RemoveIngressVisPaaSiden extends JdbcMigration {
  implicit val formats = org.json4s.DefaultFormats

  override def migrate(connection: Connection) = {
    val db = DB(connection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      allContentNodes.map(convertDocumentToNewFormat).foreach(update)
    }
  }

  def allContentNodes(implicit session: DBSession): List[V5_DBContent] = {
    sql"select id, document from contentdata".map(rs => V5_DBContent(rs.long("id"), rs.string("document"))).list().apply()
  }

  def convertDocumentToNewFormat(content: V5_DBContent): V5_DBContent = {
    val json = parse(content.document).transformField {
      case JField("introduction", JArray(intro)) => ("introduction", JArray(intro).removeField {
        case ("displayIngress", _) => true
        case _ => false
      })
    }

    V5_DBContent(content.id, write(json))
  }

  def update(content: V5_DBContent)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(content.document)

    sql"update contentdata set document = $dataObject where id = ${content.id}".update().apply
  }

}
case class V5_DBContent(id: Long, document: String)
