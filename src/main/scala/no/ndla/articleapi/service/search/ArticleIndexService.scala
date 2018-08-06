/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.search

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.indexes.IndexDefinition
import com.sksamuel.elastic4s.mappings._
import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.ArticleApiProperties
import no.ndla.articleapi.model.domain.Article
import no.ndla.articleapi.model.search.{SearchableArticle, SearchableLanguageFormats}
import no.ndla.articleapi.repository.{ArticleRepository, Repository}
import org.json4s.DefaultFormats
import org.json4s.native.Serialization.write

trait ArticleIndexService {
  this: SearchConverterService with IndexService with ArticleRepository =>
  val articleIndexService: ArticleIndexService

  class ArticleIndexService extends LazyLogging with IndexService[Article, SearchableArticle] {
    implicit val formats = SearchableLanguageFormats.JSonFormats
    override val documentType: String = ArticleApiProperties.ArticleSearchDocument
    override val searchIndex: String = ArticleApiProperties.ArticleSearchIndex
    override val repository: Repository[Article] = articleRepository

    override def createIndexRequest(domainModel: Article, indexName: String): IndexDefinition = {
      val source = write(searchConverterService.asSearchableArticle(domainModel))
      indexInto(indexName / documentType).doc(source).id(domainModel.id.get.toString)
    }

    def getMapping: MappingDefinition = {
      mapping(documentType).fields(
        List(
          intField("id"),
          keywordField("defaultTitle"),
          dateField("lastUpdated"),
          keywordField("license"),
          textField("authors").fielddata(true),
          textField("articleType").analyzer("keyword"),
          nestedField("metaImage").fields(
            keywordField("imageId"),
            keywordField("altText"),
            keywordField("language")
          )
        ) ++
          generateLanguageSupportedFieldList("title", keepRaw = true) ++
          generateLanguageSupportedFieldList("content") ++
          generateLanguageSupportedFieldList("visualElement") ++
          generateLanguageSupportedFieldList("introduction") ++
          generateLanguageSupportedFieldList("metaDescription") ++
          generateLanguageSupportedFieldList("tags")
      )
    }
  }

}
