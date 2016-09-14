/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.search

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
    Article("Bokmål artikkel", None, Some("nb")), Article("Nynorsk artikkel", None, Some("nn")),
    Article("English article", None, Some("en")), Article("Francais article", None, Some("fr")),
    Article("Deutsch Artikel", None, Some("de")), Article("Articulo espanol", None, Some("es")),
    Article("Nekonata artikolo", None, None)
  )

  val articleTags = Seq(
    ArticleTag(Seq("fugl", "fisk"), Some("nb")), ArticleTag(Seq("fugl", "fisk"), Some("nn")),
    ArticleTag(Seq("bird", "fish"), Some("en")), ArticleTag(Seq("got", "tired"), Some("fr")),
    ArticleTag(Seq("of", "translating"), Some("de")), ArticleTag(Seq("all", "of"), Some("es")),
    ArticleTag(Seq("the", "words"), None)
  )

  test("That asSearchableArticleInformation converts titles with correct language") {
    val article = ArticleInformation("1", titles, Seq(), byNcSa, Seq(), Seq(), Seq(), Seq(), 0, 1, "fagstoff")
    val searchableArticle = searchConverterService.asSearchableArticleInformation(article)
    verifyTitles(searchableArticle)
  }


  test("That asSearchableArticleInformation converts articles with correct language") {
    val article = ArticleInformation("1", Seq(), articles, byNcSa, Seq(), Seq(), Seq(), Seq(), 0, 1, "fagstoff")
    val searchableArticle = searchConverterService.asSearchableArticleInformation(article)
    verifyArticles(searchableArticle)
  }


  test("That asSearchableArticleInformation converts tags with correct language") {
    val article = ArticleInformation("1", Seq(), Seq(), byNcSa, articleTags, Seq(), Seq(), Seq(), 0, 1, "fagstoff")
    val searchableArticle = searchConverterService.asSearchableArticleInformation(article)
    verifyTags(searchableArticle)
  }


  test("That asSearchableArticleInformation converts all fields with correct language") {
    val article = ArticleInformation("1", titles, articles, byNcSa, articleTags, Seq(), Seq(), Seq(), 0, 1, "fagstoff")
    val searchableArticle = searchConverterService.asSearchableArticleInformation(article)

    verifyTitles(searchableArticle)
    verifyArticles(searchableArticle)
    verifyTags(searchableArticle)
  }

  test("That asArticleSummary converts all fields with correct language") {
    val article = ArticleInformation("1", titles, articles, byNcSa, articleTags, Seq(), Seq(), Seq(), 0, 1, "fagstoff")
    val searchableArticle = searchConverterService.asSearchableArticleInformation(article)
    val articleSummary = searchConverterService.asArticleSummary(searchableArticle)

    articleSummary.id should equal (article.id)
    articleSummary.license should equal (article.copyright.license.license)
    articleSummary.titles should equal (article.titles)
  }

  private def verifyTitles(searchableArticle: SearchableArticleInformation): Unit = {
    searchableArticle.titles.languageValues.size should equal(titles.size)
    languageValueWithLang(searchableArticle.titles, "nb") should equal(titleForLang(titles, "nb"))
    languageValueWithLang(searchableArticle.titles, "nn") should equal(titleForLang(titles, "nn"))
    languageValueWithLang(searchableArticle.titles, "en") should equal(titleForLang(titles, "en"))
    languageValueWithLang(searchableArticle.titles, "fr") should equal(titleForLang(titles, "fr"))
    languageValueWithLang(searchableArticle.titles, "de") should equal(titleForLang(titles, "de"))
    languageValueWithLang(searchableArticle.titles, "es") should equal(titleForLang(titles, "es"))
    searchableArticle.titles.languageValues.find(_.lang.isEmpty).get.value should equal(titles.find(_.language.isEmpty).get.title)
  }

  private def verifyArticles(searchableArticle: SearchableArticleInformation): Unit = {
    searchableArticle.article.languageValues.size should equal(articles.size)
    languageValueWithLang(searchableArticle.article, "nb") should equal(articleForLang(articles, "nb"))
    languageValueWithLang(searchableArticle.article, "nn") should equal(articleForLang(articles, "nn"))
    languageValueWithLang(searchableArticle.article, "en") should equal(articleForLang(articles, "en"))
    languageValueWithLang(searchableArticle.article, "fr") should equal(articleForLang(articles, "fr"))
    languageValueWithLang(searchableArticle.article, "de") should equal(articleForLang(articles, "de"))
    languageValueWithLang(searchableArticle.article, "es") should equal(articleForLang(articles, "es"))
    searchableArticle.article.languageValues.find(_.lang.isEmpty).get.value should equal(articles.find(_.language.isEmpty).get.article)
  }

  private def verifyTags(searchableArticle: SearchableArticleInformation): Unit = {
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

  private def articleForLang(articles: Seq[Article], lang: String = null): String = {
    articles.find(_.language == Option(lang)).get.article
  }

  private def tagsForLang(tags: Seq[ArticleTag], lang: String = null) = {
    tags.find(_.language == Option(lang)).get.tags
  }
}
