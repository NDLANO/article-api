/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.search

import java.util.Date

import no.ndla.articleapi.model.{ArticleTitle, _}
import no.ndla.articleapi.model.search._
import no.ndla.articleapi.{TestEnvironment, UnitSuite}

class SearchConverterServiceTest extends UnitSuite with TestEnvironment {

  override val searchConverterService = new SearchConverterService

  val byNcSa = Copyright(License("by-nc-sa", "Attribution-NonCommercial-ShareAlike", None), "Gotham City", List(Author("Forfatter", "DC Comics")))

  val titles = List(
    ArticleTitle("Bokmål tittel", Some("nb")), ArticleTitle("Nynorsk tittel", Some("nn")),
    ArticleTitle("English title", Some("en")), ArticleTitle("Titre francais", Some("fr")),
    ArticleTitle("Deutsch titel", Some("de")), ArticleTitle("Titulo espanol", Some("es")),
    ArticleTitle("Nekonata titolo", None))

  val articles = Seq(
    ArticleContent("Bokmål artikkel", None, Some("nb")), ArticleContent("Nynorsk artikkel", None, Some("nn")),
    ArticleContent("English article", None, Some("en")), ArticleContent("Francais article", None, Some("fr")),
    ArticleContent("Deutsch Artikel", None, Some("de")), ArticleContent("Articulo espanol", None, Some("es")),
    ArticleContent("Nekonata artikolo", None, None)
  )

  val articleTags = Seq(
    ArticleTag(Seq("fugl", "fisk"), Some("nb")), ArticleTag(Seq("fugl", "fisk"), Some("nn")),
    ArticleTag(Seq("bird", "fish"), Some("en")), ArticleTag(Seq("got", "tired"), Some("fr")),
    ArticleTag(Seq("of", "translating"), Some("de")), ArticleTag(Seq("all", "of"), Some("es")),
    ArticleTag(Seq("the", "words"), None)
  )

  test("That asSearchableArticle converts titles with correct language") {
    val article = Article("1", titles, Seq(), byNcSa, Seq(), Seq(), Seq(), Seq(), new Date(0), new Date(1), "fagstoff")
    val searchableArticle = searchConverterService.asSearchableArticle(article)
    verifyTitles(searchableArticle)
  }


  test("That asSearchable converts articles with correct language") {
    val article = Article("1", Seq(), articles, byNcSa, Seq(), Seq(), Seq(), Seq(), new Date(0), new Date(1), "fagstoff")
    val searchableArticle = searchConverterService.asSearchableArticle(article)
    verifyArticles(searchableArticle)
  }


  test("That asSearchable converts tags with correct language") {
    val article = Article("1", Seq(), Seq(), byNcSa, articleTags, Seq(), Seq(), Seq(), new Date(0), new Date(1), "fagstoff")
    val searchableArticle = searchConverterService.asSearchableArticle(article)
    verifyTags(searchableArticle)
  }


  test("That asSearchable converts all fields with correct language") {
    val article = Article("1", titles, articles, byNcSa, articleTags, Seq(), Seq(), Seq(), new Date(0), new Date(1), "fagstoff")
    val searchableArticle = searchConverterService.asSearchableArticle(article)

    verifyTitles(searchableArticle)
    verifyArticles(searchableArticle)
    verifyTags(searchableArticle)
  }

  test("That asArticleSummary converts all fields with correct language") {
    val article = Article("1", titles, articles, byNcSa, articleTags, Seq(), Seq(), Seq(), new Date(0), new Date(1), "fagstoff")
    val searchableArticle = searchConverterService.asSearchableArticle(article)
    val articleSummary = searchConverterService.asArticleSummary(searchableArticle)

    articleSummary.id should equal (article.id)
    articleSummary.license should equal (article.copyright.license.license)
    articleSummary.titles should equal (article.title)
  }

  private def verifyTitles(searchableArticle: SearchableArticle): Unit = {
    searchableArticle.title.languageValues.size should equal(titles.size)
    languageValueWithLang(searchableArticle.title, "nb") should equal(titleForLang(titles, "nb"))
    languageValueWithLang(searchableArticle.title, "nn") should equal(titleForLang(titles, "nn"))
    languageValueWithLang(searchableArticle.title, "en") should equal(titleForLang(titles, "en"))
    languageValueWithLang(searchableArticle.title, "fr") should equal(titleForLang(titles, "fr"))
    languageValueWithLang(searchableArticle.title, "de") should equal(titleForLang(titles, "de"))
    languageValueWithLang(searchableArticle.title, "es") should equal(titleForLang(titles, "es"))
    searchableArticle.title.languageValues.find(_.lang.isEmpty).get.value should equal(titles.find(_.language.isEmpty).get.title)
  }

  private def verifyArticles(searchableArticle: SearchableArticle): Unit = {
    searchableArticle.content.languageValues.size should equal(articles.size)
    languageValueWithLang(searchableArticle.content, "nb") should equal(articleForLang(articles, "nb"))
    languageValueWithLang(searchableArticle.content, "nn") should equal(articleForLang(articles, "nn"))
    languageValueWithLang(searchableArticle.content, "en") should equal(articleForLang(articles, "en"))
    languageValueWithLang(searchableArticle.content, "fr") should equal(articleForLang(articles, "fr"))
    languageValueWithLang(searchableArticle.content, "de") should equal(articleForLang(articles, "de"))
    languageValueWithLang(searchableArticle.content, "es") should equal(articleForLang(articles, "es"))
    searchableArticle.content.languageValues.find(_.lang.isEmpty).get.value should equal(articles.find(_.language.isEmpty).get.content)
  }

  private def verifyTags(searchableArticle: SearchableArticle): Unit = {
    languageListWithLang(searchableArticle.tags, "nb") should equal(tagsForLang(articleTags, "nb"))
    languageListWithLang(searchableArticle.tags, "nn") should equal(tagsForLang(articleTags, "nn"))
    languageListWithLang(searchableArticle.tags, "en") should equal(tagsForLang(articleTags, "en"))
    languageListWithLang(searchableArticle.tags, "fr") should equal(tagsForLang(articleTags, "fr"))
    languageListWithLang(searchableArticle.tags, "de") should equal(tagsForLang(articleTags, "de"))
    languageListWithLang(searchableArticle.tags, "es") should equal(tagsForLang(articleTags, "es"))
    languageListWithLang(searchableArticle.tags, null) should equal(tagsForLang(articleTags))
  }

  private def languageValueWithLang(languageValues: SearchableLanguageValues, lang: String = null): String = {
    languageValues.languageValues.find(_.lang == Option(lang)).get.value
  }

  private def languageListWithLang(languageList: SearchableLanguageList, lang: String = null): Seq[String] = {
    languageList.languageValues.find(_.lang == Option(lang)).get.value
  }

  private def titleForLang(titles: Seq[ArticleTitle], lang: String = null): String = {
    titles.find(_.language == Option(lang)).get.title
  }

  private def articleForLang(articles: Seq[ArticleContent], lang: String = null): String = {
    articles.find(_.language == Option(lang)).get.content
  }

  private def tagsForLang(tags: Seq[ArticleTag], lang: String = null) = {
    tags.find(_.language == Option(lang)).get.tags
  }
}
