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
import com.sksamuel.elastic4s.mappings.{MappingDefinition, NestedFieldDefinition}
import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.ArticleApiProperties
import no.ndla.articleapi.model.domain.Concept
import no.ndla.articleapi.model.domain.Language.languageAnalyzers
import no.ndla.articleapi.model.search.{SearchableArticle, SearchableLanguageFormats}
import no.ndla.articleapi.repository.{ConceptRepository, Repository}
import org.json4s.native.Serialization.write

trait ConceptIndexService {
  this: IndexService with ConceptRepository with SearchConverterService =>
  val conceptIndexService: ConceptIndexService

  class ConceptIndexService extends LazyLogging with IndexService[Concept, SearchableArticle] {
    implicit val formats = SearchableLanguageFormats.JSonFormats
    override val documentType: String = ArticleApiProperties.ConceptSearchDocument
    override val searchIndex: String = ArticleApiProperties.ConceptSearchIndex
    override val repository: Repository[Concept] = conceptRepository

    override def createIndexRequest(concept: Concept, indexName: String): IndexDefinition = {
      val source = write(searchConverterService.asSearchableConcept(concept))
      indexInto(searchIndex / documentType).doc(source).id(concept.id.get.toString)
    }

    def getMapping: MappingDefinition = {
      mapping(documentType).fields(
        intField("id"),
        languageSupportedField("title", keepRaw = true),
        languageSupportedField("content"),
        keywordField("defaultTitle")
      )
    }

    private def languageSupportedField(fieldName: String, keepRaw: Boolean = false) = {
      val languageSupportedField = new NestedFieldDefinition(fieldName).fields(
      keepRaw match {
        case true => languageAnalyzers.map(langAnalyzer => textField(langAnalyzer.lang).fielddata(true).analyzer(langAnalyzer.analyzer).fields(keywordField("raw")))
        case false => languageAnalyzers.map(langAnalyzer => textField(langAnalyzer.lang).fielddata(true).analyzer(langAnalyzer.analyzer))
      })

      languageSupportedField
    }

  }
}
