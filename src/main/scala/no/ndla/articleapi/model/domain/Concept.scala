/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.domain

import no.ndla.articleapi.ArticleApiProperties
import org.json4s.FieldSerializer
import org.json4s.FieldSerializer._
import org.json4s.native.Serialization._
import scalikejdbc._

case class Concept(id: Option[Long],
                   title: Seq[ConceptTitle],
                   content: Seq[ConceptContent]) {

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

