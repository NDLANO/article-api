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
import org.json4s.JsonAST.{JArray, JField}
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization._
import org.postgresql.util.PGobject
import scalikejdbc._

class V7__RemoveLicenseDescriptionAndUrl extends JdbcMigration {
  implicit val formats = org.json4s.DefaultFormats

  override def migrate(connection: Connection) = {
    val db = DB(connection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      allContentNodes.map(convertDocumentToNewFormat).foreach(update)
    }
  }

  def allContentNodes(implicit session: DBSession): List[V7_DBContent] = {
    sql"select id, document from contentdata".map(rs => V7_DBContent(rs.long("id"), rs.string("document"))).list().apply()
  }

  def convertDocumentToNewFormat(content: V7_DBContent): V7_DBContent = {
    val json = parse(content.document)
    val licenseStr = json \\ "copyright" \ "license" \ "license"

    val newCopyright = json \\ "copyright" mapField {
      case ("license", JObject(lic)) => ("license", licenseStr)
      case other => other
    }

    val result = json.mapField {
      case ("copyright", JObject(copyright)) => ("copyright", newCopyright)
      case other => other
    }

    V7_DBContent(content.id, write(result))
  }

  def update(content: V7_DBContent)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(content.document)

    sql"update contentdata set document = $dataObject where id = ${content.id}".update().apply
  }

}
case class V7_DBContent(id: Long, document: String)
