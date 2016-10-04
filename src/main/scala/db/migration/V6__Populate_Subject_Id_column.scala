/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 */

package db.migration

import java.sql.Connection
import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import scalikejdbc._
import no.ndla.articleapi.ComponentRegistry.migrationApiClient

class V6__Populate_Subject_Id_column extends JdbcMigration {
  implicit val formats = org.json4s.DefaultFormats

  override def migrate(connection: Connection) = {
    val db = DB(connection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      allContentNodes.map(getSubjectIds).foreach(update)
    }
  }

  def allContentNodes(implicit session: DBSession): List[String] = {
    sql"select external_id from contentdata".map(rs => rs.string("external_id")).list().apply()
  }

  def getSubjectIds(externalId: String): V6__ExternalIds = {
    V6__ExternalIds(externalId, migrationApiClient.getSubjectForNode(externalId).getOrElse(Seq()).map(_.nid))
  }

  def update(external: V6__ExternalIds)(implicit session: DBSession) = {
    val externalSubjectIds = external.external_subject_ids
    sql"update contentdata set external_subject_id = ARRAY[${externalSubjectIds}] where external_id = ${external.external_id}".update().apply
  }
}

case class V6__ExternalIds(external_id: String, external_subject_ids: Seq[String])
