/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.search

import no.ndla.articleapi.integration.JestClientFactory
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi._
import org.joda.time.DateTime


class SearchServiceTest extends UnitSuite with TestEnvironment {

  val esPort = 9200

  override val jestClient = JestClientFactory.getClient(searchServer = s"http://localhost:$esPort")

  override val searchService = new SearchService
  override val indexService = new IndexService
  override val searchConverterService = new SearchConverterService

  val byNcSa = Copyright("by-nc-sa", "Gotham City", List(Author("Forfatter", "DC Comics")))
  val publicDomain = Copyright("publicdomain", "Metropolis", List(Author("Forfatter", "Bruce Wayne")))
  val copyrighted = Copyright("copyrighted", "New York", List(Author("Forfatter", "Clark Kent")))

  val today = DateTime.now()

  val article1 = TestData.sampleArticleWithByNcSa.copy(
    id = Option(1),
    title = List(ArticleTitle("Batmen er på vift med en bil", Some("nb"))),
    introduction = List(ArticleIntroduction("Batmen", Some("nb"))),
    content = List(ArticleContent("Bilde av en <strong>bil</strong> flaggermusmann som vifter med vingene <em>bil</em>.", None, Some("nb"))),
    tags = List(ArticleTag(List("fugl"), Some("nb"))),
    created = today.minusDays(4).toDate,
    updated = today.minusDays(3).toDate)
  val article2 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(2),
    title = List(ArticleTitle("Pingvinen er ute og går", Some("nb"))),
    introduction = List(ArticleIntroduction("Pingvinen", Some("nb"))),
    content = List(ArticleContent("<p>Bilde av en</p><p> en <em>pingvin</em> som vagger borover en gate</p>", None, Some("nb"))),
    tags = List(ArticleTag(List("fugl"), Some("nb"))),
    created = today.minusDays(4).toDate,
    updated = today.minusDays(2).toDate)
  val article3 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(3),
    title = List(ArticleTitle("Donald Duck kjører bil", Some("nb"))),
    introduction = List(ArticleIntroduction("Donald Duck", Some("nb"))),
    content = List(ArticleContent("<p>Bilde av en en and</p><p> som <strong>kjører</strong> en rød bil.</p>", None, Some("nb"))),
    tags = List(ArticleTag(List("and"), Some("nb"))),
    created = today.minusDays(4).toDate,
    updated = today.minusDays(1).toDate
  )
  val article4 = TestData.sampleArticleWithCopyrighted.copy(
    id = Option(4),
    title = List(ArticleTitle("Superman er ute og flyr", Some("nb"))),
    introduction = List(ArticleIntroduction("Superman", Some("nb"))),
    content = List(ArticleContent("<p>Bilde av en flygende mann</p><p> som <strong>har</strong> superkrefter.</p>", None, Some("nb"))),
    tags = List(ArticleTag(List("supermann"), Some("nb"))),
    created = today.minusDays(4).toDate,
    updated = today.toDate
  )

  override def beforeAll = {
    indexService.createIndexWithName(ArticleApiProperties.SearchIndex)

    indexService.indexDocument(article1)
    indexService.indexDocument(article2)
    indexService.indexDocument(article3)
    indexService.indexDocument(article4)

    blockUntil(() => searchService.countDocuments() == 4)
  }

  override def afterAll() = {
    indexService.delete(Some(ArticleApiProperties.SearchIndex))
  }

  test("That getStartAtAndNumResults returns default values for None-input", ESIntegrationTest) {
    searchService.getStartAtAndNumResults(None, None) should equal((0, ArticleApiProperties.DefaultPageSize))
  }

  test("That getStartAtAndNumResults returns SEARCH_MAX_PAGE_SIZE for value greater than SEARCH_MAX_PAGE_SIZE", ESIntegrationTest) {
    searchService.getStartAtAndNumResults(None, Some(1000)) should equal((0, ArticleApiProperties.MaxPageSize))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size with default page-size", ESIntegrationTest) {
    val page = 74
    val expectedStartAt = (page - 1) * ArticleApiProperties.DefaultPageSize
    searchService.getStartAtAndNumResults(Some(page), None) should equal((expectedStartAt, ArticleApiProperties.DefaultPageSize))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size", ESIntegrationTest) {
    val page = 123
    val expectedStartAt = (page - 1) * ArticleApiProperties.DefaultPageSize
    searchService.getStartAtAndNumResults(Some(page), Some(ArticleApiProperties.DefaultPageSize)) should equal((expectedStartAt, ArticleApiProperties.DefaultPageSize))
  }

  test("That all returns all documents ordered by id ascending", ESIntegrationTest) {
    val results = searchService.all(List(), None, None, None, None, Sort.ByIdAsc)
    results.totalCount should be(3)
    results.results.head.id should be("1")
    results.results.last.id should be("3")
  }

  test("That all returns all documents ordered by id descending", ESIntegrationTest) {
    val results = searchService.all(List(), None, None, None, None, Sort.ByIdDesc)
    results.totalCount should be(3)
    results.results.head.id should be("3")
    results.results.last.id should be("1")
  }

  test("That all returns all documents ordered by title ascending", ESIntegrationTest) {
    val results = searchService.all(List(), None, None, None, None, Sort.ByTitleAsc)
    results.totalCount should be(3)
    results.results.head.id should be("1")
    results.results.last.id should be("2")
  }

  test("That all returns all documents ordered by lastUpdated descending", ESIntegrationTest) {
    val results = searchService.all(List(), None, None, None, None, Sort.ByLastUpdatedDesc)
    results.totalCount should be(3)
    results.results.head.id should be("3")
    results.results.last.id should be("1")
  }

  test("That all returns all documents ordered by lastUpdated ascending", ESIntegrationTest) {
    val results = searchService.all(List(), None, None, None, None, Sort.ByLastUpdatedAsc)
    results.totalCount should be(3)
    results.results.head.id should be("1")
    results.results.last.id should be("3")
  }

  test("That all filtering on license only returns documents with given license", ESIntegrationTest) {
    val results = searchService.all(List(), None, Some("publicdomain"), None, None, Sort.ByTitleAsc)
    results.totalCount should be(2)
    results.results.head.id should be("3")
    results.results.last.id should be("2")
  }

  test("That all filtered by id only returns documents with the given ids", ESIntegrationTest) {
    val results = searchService.all(List(1, 3), None, None, None, None, Sort.ByIdAsc)
    results.totalCount should be(2)
    results.results.head.id should be("1")
    results.results.last.id should be("3")
  }

  test("That paging returns only hits on current page and not more than page-size", ESIntegrationTest) {
    val page1 = searchService.all(List(), None, None, Some(1), Some(2), Sort.ByTitleAsc)
    val page2 = searchService.all(List(), None, None, Some(2), Some(2), Sort.ByTitleAsc)
    page1.totalCount should be(3)
    page1.page should be(1)
    page1.results.size should be(2)
    page1.results.head.id should be("1")
    page1.results.last.id should be("3")
    page2.totalCount should be(3)
    page2.page should be(2)
    page2.results.size should be(1)
    page2.results.head.id should be("2")
  }

  test("That search matches title and html-content ordered by relevance descending", ESIntegrationTest) {
    val results = searchService.matchingQuery(Seq("bil"), List(), Some("nb"), None, None, None, Sort.ByRelevanceDesc)
    results.totalCount should be(2)
    results.results.head.id should be("1")
    results.results.last.id should be("3")
  }

  test("That search combined with filter by id only returns documents matching the query with one of the given ids", ESIntegrationTest) {
    val results = searchService.matchingQuery(Seq("bil"), List(3), Some("nb"), None, None, None, Sort.ByRelevanceDesc)
    results.totalCount should be(1)
    results.results.head.id should be("3")
    results.results.last.id should be("3")
  }

  test("That search matches title", ESIntegrationTest) {
    val results = searchService.matchingQuery(Seq("Pingvinen"), List(), Some("nb"), None, None, None, Sort.ByTitleAsc)
    results.totalCount should be(1)
    results.results.head.id should be("2")
  }

  test("That search matches tags", ESIntegrationTest) {
    val results = searchService.matchingQuery(Seq("and"), List(), Some("nb"), None, None, None, Sort.ByTitleAsc)
    results.totalCount should be(1)
    results.results.head.id should be("3")
  }

  test("That search does not return superman since it has license copyrighted and license is not specified", ESIntegrationTest) {
    val results = searchService.matchingQuery(Seq("supermann"), List(), Some("nb"), None, None, None, Sort.ByTitleAsc)
    results.totalCount should be(0)
  }

  test("That search returns superman since license is specified as copyrighted", ESIntegrationTest) {
    val results = searchService.matchingQuery(Seq("supermann"), List(), Some("nb"), Some("copyrighted"), None, None, Sort.ByTitleAsc)
    results.totalCount should be(1)
    results.results.head.id should be("4")
  }

  def blockUntil(predicate: () => Boolean) = {
    var backoff = 0
    var done = false

    while (backoff <= 16 && !done) {
      if (backoff > 0) Thread.sleep(200 * backoff)
      backoff = backoff + 1
      try {
        done = predicate()
      } catch {
        case e: Throwable => println("problem while testing predicate", e)
      }
    }

    require(done, s"Failed waiting for predicate")
  }
}
