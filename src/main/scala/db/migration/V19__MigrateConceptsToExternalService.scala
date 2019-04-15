/*
 * Part of NDLA article-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package db.migration

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.ArticleApiProperties
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.DefaultFormats
import org.json4s.Extraction.decompose
import org.json4s.JsonAST.{JArray, JValue}
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.jsoup.Jsoup
import org.jsoup.nodes.Entities.EscapeMode
import org.postgresql.util.PGobject
import scalaj.http.Http
import scalikejdbc.{DB, DBSession, _}

import scala.collection.JavaConverters._
import scala.language.{implicitConversions, postfixOps}
import scala.util.{Failure, Success, Try}

class V19__MigrateConceptsToExternalService extends BaseJavaMigration with LazyLogging {

  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats
  private val explanationHost = s"${ArticleApiProperties.ApiGatewayHost}"
  private val getConceptByExternalUrl = "/concepts/api/v1/Concept/GetByExternalId"

  override def migrate(context: Context): Unit = {
    val db = DB(context.getConnection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      migrateArticleConcepts
    }
  }

  def migrateArticleConcepts(implicit session: DBSession): Unit = {
    val count = articleCount.get
    var numPagesLeft = (count / 1000) + 1
    var offset = 0L

    while (numPagesLeft > 0) {
      allArticles(offset * 1000).map {
        case (id, document) => updateArticle(convertArticle(document), id)
      }
      numPagesLeft -= 1
      offset += 1
    }
  }

  def articleCount(implicit session: DBSession): Option[Long] = {
    sql"""select count(*) from contentdata where document is not NULL"""
      .map(rs => rs.long("count"))
      .single()
      .apply()
  }

  def allArticles(offset: Long)(implicit session: DBSession): Seq[(Long, String)] = {
    sql"""
         select id, document from contentdata
         where document is not null
         order by id limit 1000 offset $offset
      """
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list
      .apply()
  }

  def getExplanationIdFromConceptId(oldConceptId: String): Option[Int] = {
    val idTry = for {
      response <- Try(
        Http(s"http://$explanationHost$getConceptByExternalUrl")
          .param("externalId", oldConceptId)
          .timeout(5000, 5000)
          .asString)
      body <- Try(parse(response.body))
      id <- Try(((body \ "data") \ "id").extract[Int])
    } yield id

    idTry match {
      case Failure(ex) =>
        logger.error(s"Some error happened when fetching new concept id from old id of '$oldConceptId'", ex)
        None
      case Success(id) =>
        logger.error(s"Works for $id")
        Some(id)
    }
  }

  private def convertContent(content: List[V17_Content]): JValue = {
    val contents = content.map(cont => {
      val document = Jsoup.parseBodyFragment(cont.content)
      document
        .outputSettings()
        .escapeMode(EscapeMode.xhtml)
        .prettyPrint(false)
        .indentAmount(0)

      for (embed <- document.select("embed[data-resource='concept']").asScala) {
        Option(embed.attr("data-content-id")).foreach(oldId => {
          getExplanationIdFromConceptId(oldId)
            .foreach(newId => embed.attr("data-content-id", newId.toString))
        })
      }

      val newContentString = document.select("body").first().html()
      cont.copy(content = newContentString)
    })

    decompose(contents)
  }

  def convertArticle(document: String): String = {
    val oldArticle = parse(document)
    val newArticle = oldArticle.mapField {
      case ("content", content: JArray) => "content" -> convertContent(content.extract[List[V17_Content]])
      case field                        => field
    }
    compact(render(newArticle))
  }

  private def updateArticle(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update contentdata set document = $dataObject where id = $id"
      .update()
      .apply
  }

  case class V17_Content(content: String, language: String)

}
