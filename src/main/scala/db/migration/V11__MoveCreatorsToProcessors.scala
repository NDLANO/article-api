/*
 * Part of NDLA article_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package db.migration

import java.util.Date

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.Extraction.decompose
import org.json4s.JValue
import org.json4s.JsonAST.JObject
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V11__MoveCreatorsToProcessors extends BaseJavaMigration {

  implicit val formats = org.json4s.DefaultFormats

  override def migrate(context: Context) = {
    val db = DB(context.getConnection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      migrateArticles
    }
  }

  def migrateArticles(implicit session: DBSession): Unit = {
    val count = countAllArticles.get
    var numPagesLeft = (count / 1000) + 1
    var offset = 0L

    while (numPagesLeft > 0) {
      allArticles(offset * 1000).map {
        case (id, document) => updateArticle(convertArticleUpdate(document), id)
      }
      numPagesLeft -= 1
      offset += 1
    }
  }

  def countAllArticles(implicit session: DBSession) = {
    sql"select count(*) from contentdata where document is not NULL".map(rs => rs.long("count")).single()
  }

  def allArticles(offset: Long)(implicit session: DBSession): Seq[(Long, String)] = {
    sql"select id, document from contentdata where document is not null order by id limit 1000 offset ${offset}"
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list()
  }

  private def convertCopyright(copyright: V11_Copyright): JValue = {
    val editorials = copyright.creators.filter(_.`type` == "Editorial")
    val newCreators = copyright.creators.toSet -- editorials.toSet
    val newProcessors = copyright.processors ++ editorials

    val newCopyright = copyright.copy(creators = newCreators.toSeq, processors = newProcessors)

    decompose(newCopyright)
  }

  def convertArticleUpdate(document: String): String = {
    val oldArticle = parse(document)

    val newArticle = oldArticle.mapField {
      case ("copyright", copyright: JObject) => "copyright" -> convertCopyright(copyright.extract[V11_Copyright])
      case x                                 => x
    }
    compact(render(newArticle))
  }

  def updateArticle(document: String, id: Long)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update contentdata set document = ${dataObject} where id = ${id}".update()
  }

  case class V11_Author(`type`: String, name: String)
  case class V11_Copyright(license: String,
                           origin: String,
                           creators: Seq[V11_Author],
                           processors: Seq[V11_Author],
                           rightsholders: Seq[V11_Author],
                           agreementId: Option[Long],
                           validFrom: Option[Date],
                           validTo: Option[Date])
}
