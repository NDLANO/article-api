/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.search

import java.util.Date

import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.model.search._
import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import no.ndla.articleapi.TestData

class ArticleSearchConverterServiceTest extends UnitSuite with TestEnvironment {

  override val searchConverterService = new SearchConverterService
  val sampleArticle = TestData.sampleArticleWithPublicDomain.copy()


  val titles = List(
    ArticleTitle("Bokmål tittel", "nb"), ArticleTitle("Nynorsk tittel", "nn"),
    ArticleTitle("English title", "en"), ArticleTitle("Titre francais", "fr"),
    ArticleTitle("Deutsch titel", "de"), ArticleTitle("Titulo espanol", "es"),
    ArticleTitle("Nekonata titolo", "unknown"))

  val articles = Seq(
    ArticleContent("Bokmål artikkel", None, "nb"), ArticleContent("Nynorsk artikkel", None, "nn"),
    ArticleContent("English article", None, "en"), ArticleContent("Francais article", None, "fr"),
    ArticleContent("Deutsch Artikel", None, "de"), ArticleContent("Articulo espanol", None, "es"),
    ArticleContent("Nekonata artikolo", None, "unknown")
  )

  val articleTags = Seq(
    ArticleTag(Seq("fugl", "fisk"), "nb"), ArticleTag(Seq("fugl", "fisk"), "nn"),
    ArticleTag(Seq("bird", "fish"), "en"), ArticleTag(Seq("got", "tired"), "fr"),
    ArticleTag(Seq("of", "translating"), "de"), ArticleTag(Seq("all", "of"), "es"),
    ArticleTag(Seq("the", "words"), "unknown")
  )

  test("That asSearchableArticle converts titles with correct language") {
    val article = TestData.sampleArticleWithByNcSa.copy(title=titles)
    val searchableArticle = searchConverterService.asSearchableArticle(article)
    verifyTitles(searchableArticle)
  }


  test("That asSearchable converts articles with correct language") {
    val article = TestData.sampleArticleWithByNcSa.copy(content=articles)
    val searchableArticle = searchConverterService.asSearchableArticle(article)
    verifyArticles(searchableArticle)
  }


  test("That asSearchable converts tags with correct language") {
    val article = TestData.sampleArticleWithByNcSa.copy(tags=articleTags)
    val searchableArticle = searchConverterService.asSearchableArticle(article)
    verifyTags(searchableArticle)
  }


  test("That asSearchable converts all fields with correct language") {
    val article = TestData.sampleArticleWithByNcSa.copy(title=titles, content=articles, tags=articleTags)
    val searchableArticle = searchConverterService.asSearchableArticle(article)

    verifyTitles(searchableArticle)
    verifyArticles(searchableArticle)
    verifyTags(searchableArticle)
  }

  test("That asArticleSummary converts all fields with correct language") {
    val article = TestData.sampleArticleWithByNcSa.copy(title=titles, content=articles, tags=articleTags)
    val searchableArticle = searchConverterService.asSearchableArticle(article)
    val articleSummary = searchConverterService.asArticleSummary(searchableArticle)

    articleSummary.id should equal (article.id.get)
    articleSummary.license should equal (article.copyright.license)
    articleSummary.title should equal (article.title)
  }

  private def verifyTitles(searchableArticle: SearchableArticle): Unit = {
    searchableArticle.title.languageValues.size should equal(titles.size)
    languageValueWithLang(searchableArticle.title, "nb") should equal(titleForLang(titles, "nb"))
    languageValueWithLang(searchableArticle.title, "nn") should equal(titleForLang(titles, "nn"))
    languageValueWithLang(searchableArticle.title, "en") should equal(titleForLang(titles, "en"))
    languageValueWithLang(searchableArticle.title, "fr") should equal(titleForLang(titles, "fr"))
    languageValueWithLang(searchableArticle.title, "de") should equal(titleForLang(titles, "de"))
    languageValueWithLang(searchableArticle.title, "es") should equal(titleForLang(titles, "es"))
    languageValueWithLang(searchableArticle.title) should equal(titleForLang(titles))
  }

  private def verifyArticles(searchableArticle: SearchableArticle): Unit = {
    searchableArticle.content.languageValues.size should equal(articles.size)
    languageValueWithLang(searchableArticle.content, "nb") should equal(articleForLang(articles, "nb"))
    languageValueWithLang(searchableArticle.content, "nn") should equal(articleForLang(articles, "nn"))
    languageValueWithLang(searchableArticle.content, "en") should equal(articleForLang(articles, "en"))
    languageValueWithLang(searchableArticle.content, "fr") should equal(articleForLang(articles, "fr"))
    languageValueWithLang(searchableArticle.content, "de") should equal(articleForLang(articles, "de"))
    languageValueWithLang(searchableArticle.content, "es") should equal(articleForLang(articles, "es"))
    languageValueWithLang(searchableArticle.content) should equal(articleForLang(articles))
  }

  private def verifyTags(searchableArticle: SearchableArticle): Unit = {
    languageListWithLang(searchableArticle.tags, "nb") should equal(tagsForLang(articleTags, "nb"))
    languageListWithLang(searchableArticle.tags, "nn") should equal(tagsForLang(articleTags, "nn"))
    languageListWithLang(searchableArticle.tags, "en") should equal(tagsForLang(articleTags, "en"))
    languageListWithLang(searchableArticle.tags, "fr") should equal(tagsForLang(articleTags, "fr"))
    languageListWithLang(searchableArticle.tags, "de") should equal(tagsForLang(articleTags, "de"))
    languageListWithLang(searchableArticle.tags, "es") should equal(tagsForLang(articleTags, "es"))
    languageListWithLang(searchableArticle.tags) should equal(tagsForLang(articleTags))
  }

  private def languageValueWithLang(languageValues: SearchableLanguageValues, lang: String = "unknown"): String = {
    languageValues.languageValues.find(_.lang == lang).get.value
  }

  private def languageListWithLang(languageList: SearchableLanguageList, lang: String = "unknown"): Seq[String] = {
    languageList.languageValues.find(_.lang == lang).get.value
  }

  private def titleForLang(titles: Seq[ArticleTitle], lang: String = "unknown"): String = {
    titles.find(_.language == lang).get.title
  }

  private def articleForLang(articles: Seq[ArticleContent], lang: String = "unknown"): String = {
    articles.find(_.language == lang).get.content
  }

  private def tagsForLang(tags: Seq[ArticleTag], lang: String = "unknown") = {
    tags.find(_.language == lang).get.tags
  }
}
