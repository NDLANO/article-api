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
import no.ndla.articleapi.model.search._
import no.ndla.network.ApplicationUrl
import org.jsoup.Jsoup

trait SearchConverterService {
  val searchConverterService: SearchConverterService

  class SearchConverterService extends LazyLogging {

    def asSearchableArticleInformation(ai: ArticleInformation): SearchableArticleInformation = {
      SearchableArticleInformation(
        id = ai.id,
        titles = SearchableLanguageValues(ai.titles.map(title => LanguageValue(title.language, title.title))),
        article = SearchableLanguageValues(ai.article.map(article => LanguageValue(article.language, Jsoup.parseBodyFragment(article.article).text()))),
        tags = SearchableLanguageList(ai.tags.map(tag => LanguageValue(tag.language, tag.tags))),
        lastUpdated = ai.updated,
        license = ai.copyright.license.license,
        authors = ai.copyright.authors.map(_.name))
    }

    def asArticleSummary(searchableArticleInformation: SearchableArticleInformation): ArticleSummary = {
      ArticleSummary(
        id = searchableArticleInformation.id,
        titles = searchableArticleInformation.titles.languageValues.map(lv => ArticleTitle(lv.value, lv.lang)),
        url = createUrlToLearningPath(searchableArticleInformation.id),
        license = searchableArticleInformation.license)
    }

    def createUrlToLearningPath(id: String): String = {
      s"${ApplicationUrl.get}$id"
    }
  }
}
