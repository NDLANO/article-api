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
import no.ndla.articleapi.model.api.{ValidationException, ValidationMessage}
import org.json4s.FieldSerializer
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
                   metaImageId: Option[String],
                   created: Date,
                   updated: Date,
                   updatedBy: String,
                   articleType: String) extends Content


object Article extends SQLSyntaxSupport[Article] {
  implicit val formats = org.json4s.DefaultFormats
  override val tableName = "contentdata"
  override val schemaName = Some(ArticleApiProperties.MetaSchema)

  def apply(lp: SyntaxProvider[Article])(rs:WrappedResultSet): Article = apply(lp.resultName)(rs)
  def apply(lp: ResultName[Article])(rs: WrappedResultSet): Article = {
    val meta = read[Article](rs.string(lp.c("document")))
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
      meta.metaImageId,
      meta.created,
      meta.updated,
      meta.updatedBy,
      meta.articleType
    )
  }

  val JSonSerializer = FieldSerializer[Article](
    ignore("id") orElse
    ignore("revision")
  )
}

object ArticleType extends Enumeration {
  val Standard = Value("standard")
  val TopicArticle = Value("topic-article")

  def all = ArticleType.values.map(_.toString).toSeq
  def valueOf(s:String): Option[ArticleType.Value] = ArticleType.values.find(_.toString == s)
  def valueOfOrError(s: String): ArticleType.Value =
    valueOf(s).getOrElse(throw new ValidationException(errors = List(ValidationMessage("articleType", s"'$s' is not a valid article type. Valid options are ${all.mkString(",")}."))))
}

case class Concept(id: Option[Long],
                   title: Seq[ConceptTitle],
                   content: Seq[ConceptContent]) extends Content {

  def title(lang: String): Option[String] = getByLanguage(title, lang)
  def content(lang: String): Option[String] = getByLanguage(content, lang)

  val supportedLanguages: Seq[String] = {
    (content ++ title)
      .map(_.language.getOrElse(Language.UnknownLanguage))
      .distinct
  }

  def supportedLanguage(lang: String): Option[String] = {
    lang match {
      case Language.NoLanguage =>
        if (supportedLanguages.contains(Language.DefaultLanguage))
          Some(Language.DefaultLanguage)
        else
          supportedLanguages.headOption
      case l if supportedLanguages.contains(l) => Some(l)
      case _ => None
    }
  }

}


object Concept extends SQLSyntaxSupport[Concept] {
  implicit val formats = org.json4s.DefaultFormats
  override val tableName = "conceptdata"
  override val schemaName = Some(ArticleApiProperties.MetaSchema)

  def apply(lp: SyntaxProvider[Concept])(rs:WrappedResultSet): Concept = apply(lp.resultName)(rs)
  def apply(lp: ResultName[Concept])(rs: WrappedResultSet): Concept = {
    val meta = read[Concept](rs.string(lp.c("document")))
    Concept(
      Some(rs.long(lp.c("id"))),
      meta.title,
      meta.content
    )
  }

  val JSonSerializer = FieldSerializer[Concept](
    ignore("id") orElse
      ignore("revision")
  )
}
