/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.search

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.model._
import no.ndla.articleapi.model.search.{SearchableArticle, SearchableArticleInformation, SearchableTags, SearchableTitles}
import no.ndla.network.ApplicationUrl
import org.jsoup.Jsoup

trait SearchConverterService {
  val searchConverterService: SearchConverterService

  class SearchConverterService extends LazyLogging {

    def asSearchableArticleInformation(articleInformation: ArticleInformation): SearchableArticleInformation = {
      SearchableArticleInformation(
        id = articleInformation.id,
        titles = asSearchableTitles(articleInformation.titles),
        article = asSearchableArticle(articleInformation.article),
        tags = asSearchableTags(articleInformation.tags),
        license = articleInformation.copyright.license.license,
        authors = articleInformation.copyright.authors.map(_.name))
    }

    def asSearchableTitles(titles: Seq[ArticleTitle]): SearchableTitles = {
      SearchableTitles(
        nb = titles.find(_.language.contains(Language.NORWEGIAN_BOKMAL)).map(_.title),
        nn = titles.find(_.language.contains(Language.NORWEGIAN_NYNORSK)).map(_.title),
        en = titles.find(_.language.contains(Language.ENGLISH)).map(_.title),
        fr = titles.find(_.language.contains(Language.FRENCH)).map(_.title),
        de = titles.find(_.language.contains(Language.GERMAN)).map(_.title),
        es = titles.find(_.language.contains(Language.SPANISH)).map(_.title),
        se = titles.find(_.language.contains(Language.SAMI)).map(_.title),
        zh = titles.find(_.language.contains(Language.CHINESE)).map(_.title),
        unknown = titles.find(_.language.isEmpty).map(_.title))
    }

    def asSearchableArticle(articles: Seq[Article]): SearchableArticle = {
      SearchableArticle(
        nb = articles.find(_.language.contains(Language.NORWEGIAN_BOKMAL)).map(article => Jsoup.parseBodyFragment(article.article).text()),
        nn = articles.find(_.language.contains(Language.NORWEGIAN_NYNORSK)).map(article => Jsoup.parseBodyFragment(article.article).text()),
        en = articles.find(_.language.contains(Language.ENGLISH)).map(article => Jsoup.parseBodyFragment(article.article).text()),
        fr = articles.find(_.language.contains(Language.FRENCH)).map(article => Jsoup.parseBodyFragment(article.article).text()),
        de = articles.find(_.language.contains(Language.GERMAN)).map(article => Jsoup.parseBodyFragment(article.article).text()),
        es = articles.find(_.language.contains(Language.SPANISH)).map(article => Jsoup.parseBodyFragment(article.article).text()),
        se = articles.find(_.language.contains(Language.SAMI)).map(article => Jsoup.parseBodyFragment(article.article).text()),
        zh = articles.find(_.language.contains(Language.CHINESE)).map(article => Jsoup.parseBodyFragment(article.article).text()),
        unknown = articles.find(_.language.isEmpty).map(article => Jsoup.parseBodyFragment(article.article).text()))
    }

    def asSearchableTags(tags: Seq[ArticleTag]): SearchableTags = {
      SearchableTags(
        nb = tags.find(_.language.contains("nb")).map(_.tags).getOrElse(Seq()),
        nn = tags.find(_.language.contains("nn")).map(_.tags).getOrElse(Seq()),
        en = tags.find(_.language.contains("en")).map(_.tags).getOrElse(Seq()),
        fr = tags.find(_.language.contains("fr")).map(_.tags).getOrElse(Seq()),
        de = tags.find(_.language.contains("de")).map(_.tags).getOrElse(Seq()),
        es = tags.find(_.language.contains("es")).map(_.tags).getOrElse(Seq()),
        se = tags.find(_.language.contains("se")).map(_.tags).getOrElse(Seq()),
        zh = tags.find(_.language.contains("zh")).map(_.tags).getOrElse(Seq()),
        unknown = tags.find(_.language.isEmpty).map(_.tags).getOrElse(Seq()))
    }

    def asArticleSummary(searchableArticleInformation: SearchableArticleInformation): ArticleSummary = {
      ArticleSummary(
        id = searchableArticleInformation.id,
        titles = asApiTitle(searchableArticleInformation.titles),
        url = createUrlToLearningPath(searchableArticleInformation.id),
        license = searchableArticleInformation.license)
    }

    def asApiTitle(titles: SearchableTitles): Seq[ArticleTitle] = {
      List((titles.zh, Some(Language.CHINESE)),
        (titles.en, Some(Language.ENGLISH)),
        (titles.fr, Some(Language.FRENCH)),
        (titles.de, Some(Language.GERMAN)),
        (titles.nb, Some(Language.NORWEGIAN_BOKMAL)),
        (titles.nn, Some(Language.NORWEGIAN_NYNORSK)),
        (titles.se, Some(Language.SAMI)),
        (titles.es, Some(Language.SPANISH)),
        (titles.unknown, None)
      ).filter(_._1.isDefined).map(tuple => ArticleTitle(tuple._1.get, tuple._2))
    }

    def createUrlToLearningPath(id: String): String = {
      s"${ApplicationUrl.get}$id"
    }
  }
}
