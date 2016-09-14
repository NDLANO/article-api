/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.search

import com.sksamuel.elastic4s.testkit.ElasticSugar
import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import no.ndla.articleapi.model._


class SearchServiceTest extends UnitSuite with TestEnvironment with ElasticSugar {

  override val elasticClient = client
  override val searchService = new SearchService
  override val elasticContentIndex = new ElasticContentIndex
  override val searchConverterService = new SearchConverterService

  val byNcSa = Copyright(License("by-nc-sa", "Attribution-NonCommercial-ShareAlike", None), "Gotham City", List(Author("Forfatter", "DC Comics")))
  val publicDomain = Copyright(License("publicdomain", "Public Domain", None), "Metropolis", List(Author("Forfatter", "Bruce Wayne")))

  val article1 = ArticleInformation("1", List(ArticleTitle("Batmen er på vift med en bil", Some("nb"))), List(Article("Bilde av en <strong>bil</strong> flaggermusmann som vifter med vingene <em>bil</em>.", None, Some("nb"))), byNcSa, List(ArticleTag(List("fugl"), Some("nb"))), List(), Seq(), Seq(), 0, 1, "fagstoff")
  val article2 = ArticleInformation("2", List(ArticleTitle("Pingvinen er ute og går", Some("nb"))), List(Article("<p>Bilde av en</p><p> en <em>pingvin</em> som vagger borover en gate</p>", None, Some("nb"))), publicDomain, List(ArticleTag(List("fugl"), Some("nb"))), List(), Seq(), Seq(), 0, 1, "fagstoff")
  val article3 = ArticleInformation("3", List(ArticleTitle("Donald Duck kjører bil", Some("nb"))), List(Article("<p>Bilde av en en and</p><p> som <strong>kjører</strong> en rød bil.</p>", None, Some("nb"))), publicDomain, List(ArticleTag(List("and"), Some("nb"))), List(), Seq(), Seq(), 0, 1, "fagstoff")

  override def beforeAll = {
    val indexName = elasticContentIndex.create()
    elasticContentIndex.updateAliasTarget(None, indexName)
    elasticContentIndex.indexDocuments(List(article1, article2, article3), indexName)

    blockUntilCount(3, indexName)
  }

  test("That getStartAtAndNumResults returns default values for None-input") {
    searchService.getStartAtAndNumResults(None, None) should equal((0, DEFAULT_PAGE_SIZE))
  }

  test("That getStartAtAndNumResults returns SEARCH_MAX_PAGE_SIZE for value greater than SEARCH_MAX_PAGE_SIZE") {
    searchService.getStartAtAndNumResults(None, Some(1000)) should equal((0, MAX_PAGE_SIZE))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size with default page-size") {
    val page = 74
    val expectedStartAt = (page - 1) * DEFAULT_PAGE_SIZE
    searchService.getStartAtAndNumResults(Some(page), None) should equal((expectedStartAt, DEFAULT_PAGE_SIZE))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size") {
    val page = 123
    val pageSize = 321
    val expectedStartAt = (page - 1) * pageSize
    searchService.getStartAtAndNumResults(Some(page), Some(pageSize)) should equal((expectedStartAt, pageSize))
  }

  test("That all returns all documents ordered by title ascending") {
    val results = searchService.all(None, None, None, None, Sort.ByTitleAsc)
    results.totalCount should be (3)
    results.results.head.id should be ("1")
    results.results.last.id should be ("2")
  }

  test("That all filtering on license only returns documents with given license") {
    val results = searchService.all(None, Some("publicdomain"), None, None, Sort.ByTitleAsc)
    results.totalCount should be (2)
    results.results.head.id should be ("3")
    results.results.last.id should be ("2")
  }

  test("That paging returns only hits on current page and not more than page-size") {
    val page1 = searchService.all(None, None, Some(1), Some(2), Sort.ByTitleAsc)
    val page2 = searchService.all(None, None, Some(2), Some(2), Sort.ByTitleAsc)
    page1.totalCount should be (3)
    page1.page should be (1)
    page1.results.size should be (2)
    page1.results.head.id should be ("1")
    page1.results.last.id should be ("3")
    page2.totalCount should be (3)
    page2.page should be (2)
    page2.results.size should be (1)
    page2.results.head.id should be ("2")
  }

  test("That search matches title and html-content ordered by relevance descending") {
    val results = searchService.matchingQuery(Seq("bil"), Some("nb"), None, None, None, Sort.ByRelevanceDesc)
    results.totalCount should be (2)
    results.results.head.id should be ("1")
    results.results.last.id should be ("3")
  }

  test("That search matches title") {
    val results = searchService.matchingQuery(Seq("Pingvinen"), Some("nb"), None, None, None, Sort.ByTitleAsc)
    results.totalCount should be (1)
    results.results.head.id should be ("2")
  }

  test("That search matches tags") {
    val results = searchService.matchingQuery(Seq("and"), Some("nb"), None, None, None, Sort.ByTitleAsc)
    results.totalCount should be (1)
    results.results.head.id should be ("3")
  }
}
