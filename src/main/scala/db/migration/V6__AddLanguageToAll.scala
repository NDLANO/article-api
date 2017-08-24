/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package db.migration

import java.sql.Connection
import java.util.Date

import no.ndla.articleapi.model.domain.Language
import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import org.json4s.FieldSerializer
import org.json4s.FieldSerializer.ignore
import org.json4s.native.Serialization.{read, write}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V6__AddLanguageToAll extends JdbcMigration {

  implicit val formats = org.json4s.DefaultFormats + FieldSerializer[V6_Article](ignore("id") orElse ignore("revision"))

  override def migrate(connection: Connection) = {
    val db = DB(connection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      val count = countAllArticles.get
      var numPagesLeft = (count / 1000) + 1
      var offset = 0L

      while (numPagesLeft > 0) {
        allArticles(offset * 1000).map(convertArticleUpdate).foreach(update)
        numPagesLeft -= 1
        offset += 1
      }
    }
  }

  def countAllArticles(implicit session: DBSession) = {
    sql"select count(*) from contentdata".map(rs => rs.long("count")).single().apply()
  }

  def allArticles(offset: Long)(implicit session: DBSession): Seq[V6_Article] = {
    sql"select id, revision, document from contentdata order by id limit 1000 offset ${offset}".map(rs => {
      val meta = read[V6_Article](rs.string("document"))
      meta.copy(
        id = Some(rs.long("id")),
        revision = Some(rs.int("revision")))

    }).list.apply()
  }

  def convertArticleUpdate(articleMeta: V6_Article): V6_Article = {
    articleMeta.copy(
      title = articleMeta.title.map(t => V6_ArticleTitle(t.title, Some(Language.languageOrUnknown(t.language)))),
      content = articleMeta.content.map(c => V6_ArticleContent(c.content, c.footNotes, Some(Language.languageOrUnknown(c.language)))),
      tags = articleMeta.tags.map(t => V6_ArticleTag(t.tags, Some(Language.languageOrUnknown(t.language)))),
      visualElement = articleMeta.visualElement.map(v => V6_VisualElement(v.resource, Some(Language.languageOrUnknown(v.language)))),
      introduction = articleMeta.introduction.map(i => V6_ArticleIntroduction(i.introduction, Some(Language.languageOrUnknown(i.language)))),
      metaDescription = articleMeta.metaDescription.map(m => V6_ArticleMetaDescription(m.content, Some(Language.languageOrUnknown(m.language))))
    )
  }


  def update(articleMeta: V6_Article)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(write(articleMeta))

    sql"update contentdata set document = $dataObject where id = ${articleMeta.id}".update().apply
  }

}

case class V6_ArticleTitle(title: String, language: Option[String])
case class V6_ArticleContent(content: String, footNotes: Option[Map[String, V6_FootNoteItem]], language: Option[String])
case class V6_ArticleTag(tags: Seq[String], language: Option[String])
case class V6_VisualElement(resource: String, language: Option[String])
case class V6_ArticleIntroduction(introduction: String, language: Option[String])
case class V6_ArticleMetaDescription(content: String, language: Option[String])
case class V6_RequiredLibrary(mediaType: String, name: String, url: String)
case class V6_Copyright(license: String, origin: String, authors: Seq[V6_Author])
case class V6_Author(`type`: String, name: String)
case class V6_FootNoteItem(title: String, `type`: String, year: String, edition: String, publisher: String, authors: Seq[String])

case class V6_Article(id: Option[Long],
                      revision: Option[Int],
                      title: Seq[V6_ArticleTitle],
                      content: Seq[V6_ArticleContent],
                      copyright: V6_Copyright,
                      tags: Seq[V6_ArticleTag],
                      requiredLibraries: Seq[V6_RequiredLibrary],
                      visualElement: Seq[V6_VisualElement],
                      introduction: Seq[V6_ArticleIntroduction],
                      metaDescription: Seq[V6_ArticleMetaDescription],
                      metaImageId: Option[String],
                      created: Date,
                      updated: Date,
                      updatedBy: String,
                      articleType: String)