/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.search

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.model.api.{ArticleV2 => ApiArticleV2}
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.model.search._
import no.ndla.articleapi.service.ConverterService
import no.ndla.network.ApplicationUrl
import org.joda.time.DateTime
import org.jsoup.Jsoup

trait SearchConverterService {
  this: ConverterService =>
  val searchConverterService: SearchConverterService

  class SearchConverterService extends LazyLogging {

    def asSearchableArticle(ai: Article): SearchableArticle = {
      val articleWithAgreement = converterService.withAgreementCopyright(ai)

      val defaultTitle = articleWithAgreement.title.sortBy(title => {
        val languagePriority = Language.languageAnalyzers.map(la => la.lang).reverse
        languagePriority.indexOf(title.language)
      }).lastOption

      SearchableArticle(
        id = articleWithAgreement.id.get,
        title = SearchableLanguageValues(articleWithAgreement.title.map(title => LanguageValue(title.language, title.title))),
        visualElement = SearchableLanguageValues(articleWithAgreement.visualElement.map(visual => LanguageValue(visual.language, visual.resource))),
        introduction = SearchableLanguageValues(articleWithAgreement.introduction.map(intro => LanguageValue(intro.language, intro.introduction))),
        metaDescription = SearchableLanguageValues(articleWithAgreement.metaDescription.map(meta => LanguageValue(meta.language, meta.content))),
        metaImage = articleWithAgreement.metaImage,
        content = SearchableLanguageValues(articleWithAgreement.content.map(article => LanguageValue(article.language, Jsoup.parseBodyFragment(article.content).text()))),
        tags = SearchableLanguageList(articleWithAgreement.tags.map(tag => LanguageValue(tag.language, tag.tags))),
        lastUpdated = new DateTime(articleWithAgreement.updated),
        license = articleWithAgreement.copyright.license,
        authors = articleWithAgreement.copyright.creators.map(_.name) ++ articleWithAgreement.copyright.processors.map(_.name) ++ articleWithAgreement.copyright.rightsholders.map(_.name),
        articleType = articleWithAgreement.articleType,
        defaultTitle = defaultTitle.map(t => t.title)
      )
    }

    def asArticleSummary(searchableArticle: SearchableArticle): ArticleSummary = {
      ArticleSummary(
        id = searchableArticle.id,
        title = searchableArticle.title.languageValues.map(lv => ArticleTitle(lv.value, lv.language)),
        visualElement = searchableArticle.visualElement.languageValues.map(lv => VisualElement(lv.value, lv.language)),
        introduction = searchableArticle.introduction.languageValues.map(lv => ArticleIntroduction(lv.value, lv.language)),
        url = createUrlToArticle(searchableArticle.id),
        license = searchableArticle.license)
    }

    def createUrlToArticle(id: Long): String = {
      s"${ApplicationUrl.get}$id"
    }

    def asSearchableConcept(c: Concept): SearchableConcept = {

      val defaultTitle = c.title.sortBy(title => {
        val languagePriority = Language.languageAnalyzers.map(la => la.lang).reverse
        languagePriority.indexOf(title.language)
      }).lastOption

      SearchableConcept(
        c.id.get,
        SearchableLanguageValues(c.title.map(title => LanguageValue(title.language, title.title))),
        SearchableLanguageValues(c.content.map(content => LanguageValue(content.language, content.content))),
        defaultTitle.map(t => t.title)
      )
    }

  }
}
