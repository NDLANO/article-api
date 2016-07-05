package db.migration

import java.sql.Connection
import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization._
import org.postgresql.util.PGobject
import scalikejdbc._

class V2__ChangeTagStructure extends JdbcMigration {

  implicit val formats = org.json4s.DefaultFormats

  override def migrate(connection: Connection) = {
    val db = DB(connection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      allContentNodes.flatMap(convertTagsToNewFormat).foreach(update)
    }
  }

  def allContentNodes(implicit session: DBSession): List[V2_DBContent] = {
    sql"select id, document from contentdata".map(rs => V2_DBContent(rs.long("id"), rs.string("document"))).list().apply()
  }

  def convertTagsToNewFormat(content: V2_DBContent): Option[V2_DBContent] = {
    val json = parse(content.document)
    val tags = json \\ "tags"

    val oldTagsOpt: Option[List[V2_OldTag]] = tags.extractOpt[List[V2_OldTag]]
    oldTagsOpt match {
      case None => None
      case Some(oldTags) => {
        val newTags = oldTags.groupBy(_.language).map(entry => (entry._1, entry._2.map(_.tag))).map(entr => V2_ContentTags(entr._2, entr._1))
        Some(content.copy(document = compact(render(json.replace(List("tags"), parse(write(newTags)))))))
      }
    }
  }

  def update(content: V2_DBContent)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(content.document)

    sql"update contentdata set document = $dataObject where id = ${content.id}".update().apply
  }

}

case class V2_ContentTags(tag: Seq[String], language:Option[String])
case class V2_OldTag(tag: String, language: Option[String])
case class V2_DBContent(id: Long, document: String)
