/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.domain

import java.util.Date

import no.ndla.articleapi.ArticleApiProperties
import org.json4s.FieldSerializer
import org.json4s.FieldSerializer._
import org.json4s.native.Serialization._
import scalikejdbc._

case class Article(id: Option[Long],
                   revision: Option[Int],
                   title: Seq[ArticleTitle],
                   content: Seq[ArticleContent],
                   copyright: Copyright,
                   tags: Seq[ArticleTag],
                   requiredLibraries: Seq[RequiredLibrary],
                   visualElement: Seq[VisualElement],
                   introduction: Seq[ArticleIntroduction],
                   metaDescription: Seq[ArticleMetaDescription],
                   metaImageId: Option[String],
                   created: Date,
                   updated: Date,
                   contentType: String)


object Article extends SQLSyntaxSupport[Article] {
  implicit val formats = org.json4s.DefaultFormats
  override val tableName = "contentdata"
  override val schemaName = Some(ArticleApiProperties.MetaSchema)

  def apply(lp: SyntaxProvider[Article])(rs:WrappedResultSet): Article = apply(lp.resultName)(rs)
  def apply(lp: ResultName[Article])(rs: WrappedResultSet): Article = {
    val meta = read[Article](rs.string(lp.c("document")))
    Article(Some(rs.long(lp.c("id"))), Some(rs.int(lp.c("revision"))), meta.title, meta.content, meta.copyright, meta.tags, meta.requiredLibraries,
      meta.visualElement, meta.introduction, meta.metaDescription, meta.metaImageId, meta.created, meta.updated, meta.contentType)
  }

  val JSonSerializer = FieldSerializer[Article](
    ignore("id") orElse
    ignore("revision")
  )
}
