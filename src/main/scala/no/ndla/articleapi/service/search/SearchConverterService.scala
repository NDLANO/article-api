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

    def asSearchableArticle(ai: Article): SearchableArticle = {
      SearchableArticle(
        id = ai.id,
        title = SearchableLanguageValues(ai.title.map(title => LanguageValue(title.language, title.title))),
        content = SearchableLanguageValues(ai.content.map(article => LanguageValue(article.language, Jsoup.parseBodyFragment(article.content).text()))),
        tags = SearchableLanguageList(ai.tags.map(tag => LanguageValue(tag.language, tag.tags))),
        lastUpdated = ai.updated,
        license = ai.copyright.license.license,
        authors = ai.copyright.authors.map(_.name))
    }

    def asArticleSummary(searchableArticle: SearchableArticle): ArticleSummary = {
      ArticleSummary(
        id = searchableArticle.id,
        titles = searchableArticle.title.languageValues.map(lv => ArticleTitle(lv.value, lv.lang)),
        url = createUrlToLearningPath(searchableArticle.id),
        license = searchableArticle.license)
    }

    def createUrlToLearningPath(id: String): String = {
      s"${ApplicationUrl.get}$id"
    }
  }
}
