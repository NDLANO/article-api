package db.migration

import java.sql.Connection

import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import org.json4s.JsonAST.{JArray, JObject}
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization._
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession}
import scalikejdbc._

class V3__ChangeContentName extends JdbcMigration {

  implicit val formats = org.json4s.DefaultFormats

  override def migrate(connection: Connection) = {
    val db = DB(connection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      allContentNodes.flatMap(convertDocumentToNewFormat).foreach(update)
    }
  }

  def allContentNodes(implicit session: DBSession): List[V3_DBContent] = {
    sql"select id, document from contentdata".map(rs => V3_DBContent(rs.long("id"), rs.string("document"))).list().apply()
  }

  def convertDocumentToNewFormat(content: V3_DBContent): Option[V3_DBContent] = {
    val json = parse(content.document).mapField {
      case ("content", JArray(x)) => ("article", JArray(x))
      case x => x
    }
    val article = json \\ "article"

    val oldContentOpt: Option[List[V3_OldContent]] = article.extractOpt[List[V3_OldContent]]
    oldContentOpt match {
      case None => None
      case Some(oldContent) => {
        val newArticle = oldContent.map(x => V3_Article(x.content, x.footNotes, x.language))
        Some(content.copy(document = compact(render(json.replace(List("article"), parse(write(newArticle)))))))
      }
    }
  }

  def update(content: V3_DBContent)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(content.document)

    sql"update contentdata set document = $dataObject where id = ${content.id}".update().apply
  }

  case class V3_DBContent(id: Long, document: String)
  case class V3_OldContent(content: String, footNotes: Option[Map[String, V3_FootNoteItem]], language: Option[String])
  case class V3_Article(article: String, footNotes: Option[Map[String, V3_FootNoteItem]], language: Option[String])
  case class V3_FootNoteItem(title: String, `type`: String, year: String, edition: String, publisher: String, authors: Seq[String])
}
