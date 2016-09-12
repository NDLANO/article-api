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
      def find(lang: String = null) = titles.find(_.language == Option(lang)).map(_.title)

      SearchableTitles(
        nb = find(Language.NORWEGIAN_BOKMAL),
        nn = find(Language.NORWEGIAN_NYNORSK),
        en = find(Language.ENGLISH),
        fr = find(Language.FRENCH),
        de = find(Language.GERMAN),
        es = find(Language.SPANISH),
        se = find(Language.SAMI),
        zh = find(Language.CHINESE),
        unknown = find())
    }

    def asSearchableArticle(articles: Seq[Article]): SearchableArticle = {
      def find(lang: String = null) = articles.find(_.language == Option(lang)).map(article => Jsoup.parseBodyFragment(article.article).text())

      SearchableArticle(
        nb = find(Language.NORWEGIAN_BOKMAL),
        nn = find(Language.NORWEGIAN_NYNORSK),
        en = find(Language.ENGLISH),
        fr = find(Language.FRENCH),
        de = find(Language.GERMAN),
        es = find(Language.SPANISH),
        se = find(Language.SAMI),
        zh = find(Language.CHINESE),
        unknown = find())
    }

    def asSearchableTags(tags: Seq[ArticleTag]): SearchableTags = {
      def find(lang: String = null) = tags.find(_.language == Option(lang)).map(_.tags).getOrElse(Seq())

      SearchableTags(
        nb = find(Language.NORWEGIAN_BOKMAL),
        nn = find(Language.NORWEGIAN_NYNORSK),
        en = find(Language.ENGLISH),
        fr = find(Language.FRENCH),
        de = find(Language.GERMAN),
        es = find(Language.SPANISH),
        se = find(Language.SAMI),
        zh = find(Language.CHINESE),
        unknown = find())
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
