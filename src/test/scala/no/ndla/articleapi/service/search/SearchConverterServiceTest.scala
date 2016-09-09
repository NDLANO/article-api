/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.search

import no.ndla.articleapi.model._
import no.ndla.articleapi.model.search.SearchableTitles
import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import org.jsoup.Jsoup

class SearchConverterServiceTest extends UnitSuite with TestEnvironment {

  override val searchConverterService = new SearchConverterService

  val byNcSa = Copyright(License("by-nc-sa", "Attribution-NonCommercial-ShareAlike", None), "Gotham City", List(Author("Forfatter", "DC Comics")))
  val publicDomain = Copyright(License("publicdomain", "Public Domain", None), "Metropolis", List(Author("Forfatter", "Bruce Wayne")))

  val article1 = ArticleInformation("1", List(ArticleTitle("Batmen er på vift med en bil", Some("nb"))), List(Article("Bilde av en <strong>bil</strong> flaggermusmann som vifter med vingene <em>bil</em>.", None, Some("nb"))), byNcSa, List(ArticleTag(List("fugl"), Some("nb"))), List())
  val article2 = ArticleInformation("2", List(ArticleTitle("Pingvinen er ute og går", Some("nb"))), List(Article("<p>Bilde av en</p><p> en <em>pingvin</em> som vagger borover en gate</p>", None, Some("nb"))), publicDomain, List(ArticleTag(List("fugl"), Some("nb"))), List())
  val article3 = ArticleInformation("3", List(ArticleTitle("Donald Duck kjører bil", Some("nb"))), List(Article("<p>Bilde av en en and</p><p> som <strong>kjører</strong> en rød bil.</p>", None, Some("nb"))), publicDomain, List(ArticleTag(List("and"), Some("nb"))), List())

  test("That asSearchableArticleInformation converts all fields") {
    val searchableArticle = searchConverterService.asSearchableArticleInformation(article1)
    searchableArticle.id should be ("1")
    searchableArticle.titles.nb should equal (Some(article1.titles.head.title))
    searchableArticle.license should equal(article1.copyright.license.license)
    searchableArticle.article.nb should equal (Some(Jsoup.parseBodyFragment(article1.article.head.article).text()))
    searchableArticle.authors.head should equal(article1.copyright.authors.head.name)
    searchableArticle.tags.nb should equal (article1.tags.flatMap(_.tags))
  }

  test("That asSearchableArticle removes html-tags") {
    val searchableArticle = searchConverterService.asSearchableArticle(List(Article("<strong>Heisann</strong>", None, Some("nb"))))
    searchableArticle.nb should equal(Some("Heisann"))
  }

  test("That asSearchableTitles converts language to correct place") {
    val searchableTitles = searchConverterService.asSearchableTitles(List(
      ArticleTitle("Tittel", Some("nb")),
      ArticleTitle("Title", Some("en"))))

    searchableTitles.nb should equal(Some("Tittel"))
    searchableTitles.en should equal(Some("Title"))
    searchableTitles.de should be (None)
    searchableTitles.nn should be (None)
    searchableTitles.es should be (None)
    searchableTitles.zh should be (None)
    searchableTitles.fr should be (None)
    searchableTitles.se should be (None)
    searchableTitles.unknown should be (None)
  }

  test("That asSearchableArticle converts language to correct place") {
    val searchableArticles = searchConverterService.asSearchableArticle(List(
      Article("Artikkel på bokmål", None, Some("nb")),
      Article("Artikkel på nynorsk", None, Some("nn"))))

    searchableArticles.nb should equal(Some("Artikkel på bokmål"))
    searchableArticles.nn should equal(Some("Artikkel på nynorsk"))
    searchableArticles.de should be (None)
    searchableArticles.en should be (None)
    searchableArticles.es should be (None)
    searchableArticles.zh should be (None)
    searchableArticles.fr should be (None)
    searchableArticles.se should be (None)
    searchableArticles.unknown should be (None)
  }

  test("That as searchable tags converts all languages specified") {
    val searchableTags = searchConverterService.asSearchableTags(List(
      ArticleTag(List("fugl", "fisk", "føll"), Some("nb")),
      ArticleTag(List("bird", "fish", "foal"), Some("en"))))

    searchableTags.nb should equal(Seq("fugl", "fisk", "føll"))
    searchableTags.en should equal(Seq("bird", "fish", "foal"))
    searchableTags.de should be (Seq())
    searchableTags.nn should be (Seq())
    searchableTags.es should be (Seq())
    searchableTags.zh should be (Seq())
    searchableTags.fr should be (Seq())
    searchableTags.se should be (Seq())
    searchableTags.unknown should be (Seq())
  }


  test("That asApiTitle only converts specified languages") {
  val searchableTitles = SearchableTitles(
    nb = Some("Tittel bokmål"),
    nn = Some("Tittel nynorsk"),
    en = None, fr = None, de = None, es = None, se = None, zh = None, unknown = None)

    val apiTitles = searchConverterService.asApiTitle(searchableTitles)
    apiTitles.length should be (2)
    apiTitles.filter(_.language.contains("nb")).head should equal (ArticleTitle("Tittel bokmål", Some("nb")))
    apiTitles.filter(_.language.contains("nn")).head should equal (ArticleTitle("Tittel nynorsk", Some("nn")))
  }

}
