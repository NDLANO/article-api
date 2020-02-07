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
import no.ndla.validation.{ValidationException, ValidationMessage}
import org.json4s.{DefaultFormats, FieldSerializer}
import org.json4s.FieldSerializer._
import org.json4s.native.Serialization._
import scalikejdbc._

sealed trait Content {
  def id: Option[Long]
}

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
                   metaImage: Seq[ArticleMetaImage],
                   created: Date,
                   updated: Date,
                   updatedBy: String,
                   published: Date,
                   articleType: String,
                   competences: Seq[String])
    extends Content

object Article extends SQLSyntaxSupport[Article] {
  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats
  override val tableName = "contentdata"
  override val schemaName = Some(ArticleApiProperties.MetaSchema)

  def apply(id: Option[Long],
            revision: Option[Int],
            title: Seq[ArticleTitle],
            content: Seq[ArticleContent],
            copyright: Copyright,
            tags: Seq[ArticleTag],
            requiredLibraries: Seq[RequiredLibrary],
            visualElement: Seq[VisualElement],
            introduction: Seq[ArticleIntroduction],
            metaDescription: Seq[ArticleMetaDescription],
            metaImage: Seq[ArticleMetaImage],
            created: Date,
            updated: Date,
            updatedBy: String,
            published: Date,
            articleType: String,
            competences: Seq[String] = Nil): Article = {

    new Article(
      id,
      revision,
      title,
      content,
      copyright,
      tags,
      requiredLibraries,
      visualElement,
      introduction,
      metaDescription,
      metaImage,
      created,
      updated,
      updatedBy,
      published,
      articleType,
      competences
    )
  }

  def fromResultSet(lp: SyntaxProvider[Article])(rs: WrappedResultSet): Article = fromResultSet(lp.resultName)(rs)

  def fromResultSet(lp: ResultName[Article])(rs: WrappedResultSet): Article = {
    implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats

    val jsonStr = rs.string(lp.c("document"))

    val meta = read[Article](jsonStr)
    Article(
      Some(rs.long(lp.c("id"))),
      Some(rs.int(lp.c("revision"))),
      meta.title,
      meta.content,
      meta.copyright,
      meta.tags,
      meta.requiredLibraries,
      meta.visualElement,
      meta.introduction,
      meta.metaDescription,
      meta.metaImage,
      meta.created,
      meta.updated,
      meta.updatedBy,
      meta.published,
      meta.articleType,
      meta.competences
    )
  }

  val JSonSerializer: FieldSerializer[Article] = FieldSerializer[Article](
    ignore("id")
  )
}

object ArticleType extends Enumeration {
  val Standard: ArticleType.Value = Value("standard")
  val TopicArticle: ArticleType.Value = Value("topic-article")

  def all: Seq[String] = ArticleType.values.map(_.toString).toSeq
  def valueOf(s: String): Option[ArticleType.Value] = ArticleType.values.find(_.toString == s)

  def valueOfOrError(s: String): ArticleType.Value =
    valueOf(s).getOrElse(throw new ValidationException(errors = List(
      ValidationMessage("articleType", s"'$s' is not a valid article type. Valid options are ${all.mkString(",")}."))))
}
