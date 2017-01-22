/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi

import no.ndla.articleapi.integration.LanguageContent
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.ArticleApiProperties.resourceHtmlEmbedTag
import no.ndla.articleapi.model.api
import org.joda.time.DateTime

object TestData {
  private val publicDomainCopyright= Copyright("publicdomain", "", List())
  private val byNcSaCopyright = Copyright("by-nc-sa", "Gotham City", List(Author("Forfatter", "DC Comics")))
  private val copyrighted = Copyright("copyrighted", "New York", List(Author("Forfatter", "Clark Kent")))

  private val embedUrl = "http://www.example.org"

  val sampleArticleWithPublicDomain = Article(
    Option(1),
    Seq(ArticleTitle("test", Option("en"))),
    Seq(ArticleContent("<article><div>test</div></article>", None, Option("en"))),
    publicDomainCopyright,
    Seq(),
    Seq(),
    Seq(),
    Seq(),
    Seq(),
    None,
    DateTime.now().minusDays(4).toDate,
    DateTime.now().minusDays(2).toDate,
    "fagstoff")

  val sampleArticleWithByNcSa = sampleArticleWithPublicDomain.copy(copyright=byNcSaCopyright)
  val sampleArticleWithCopyrighted = sampleArticleWithPublicDomain.copy(copyright=copyrighted )

  val updatedArticle = api.UpdatedArticle(
    Seq(api.ArticleTitle("test", Option("en"))),
    Seq(api.ArticleContent("<article><div>test</div></article>", None, Option("en"))),
    Seq(),
    None,
    None,
    None,
    None,
    api.Copyright(api.License("publicdomain", None, None), "", Seq()),
    None,
    "fagstoff"
  )

  val newArticle = api.NewArticle(
    Seq(api.ArticleTitle("test", Option("en"))),
    Seq(api.ArticleContent("<article><div>test</div></article>", None, Option("en"))),
    Seq(),
    None,
    None,
    None,
    None,
    api.Copyright(api.License("publicdomain", None, None), "", Seq()),
    None,
    "fagstoff"
  )

  val (nodeId, nodeId2) = ("1234", "4321")
  val sampleTitle = ArticleTitle("title", Some("en"))
  val sampleContent = LanguageContent(nodeId, nodeId, "sample content", "metadescription",  Some("en"))
  val sampleTranslationContent = sampleContent.copy(tnid=nodeId2)

  val visualElement = VisualElement(s"""<$resourceHtmlEmbedTag  data-align="" data-alt="" data-caption="" data-id="1" data-resource="image" data-resource_id="1" data-size="" />""", Some("nb"))
}


