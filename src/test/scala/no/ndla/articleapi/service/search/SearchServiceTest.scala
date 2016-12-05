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
import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.node.{Node, NodeBuilder}
import org.joda.time.DateTime

import scala.reflect.io.Path
import scala.util.Random

class SearchServiceTest extends UnitSuite with TestEnvironment {

  val esHttpPort = new Random(System.currentTimeMillis()).nextInt(30000 - 20000) + 20000
  val esDataDir = "esTestData"
  var esNode: Node = _

  override val jestClient = JestClientFactory.getClient(searchServer = s"http://localhost:$esHttpPort")

  override val searchService = new SearchService
  override val indexService = new IndexService
  override val searchConverterService = new SearchConverterService

  val byNcSa = Copyright("by-nc-sa", "Gotham City", List(Author("Forfatter", "DC Comics")))
  val publicDomain = Copyright("publicdomain", "Metropolis", List(Author("Forfatter", "Bruce Wayne")))
  val copyrighted = Copyright("copyrighted", "New York", List(Author("Forfatter", "Clark Kent")))

  val today = DateTime.now()

  val article1 = Article(Some(1), List(ArticleTitle("Batmen er på vift med en bil", Some("nb"))), List(ArticleContent("Bilde av en <strong>bil</strong> flaggermusmann som vifter med vingene <em>bil</em>.", None, Some("nb"))), byNcSa, List(ArticleTag(List("fugl"), Some("nb"))), List(), Seq(), Seq(), today.minusDays(4).toDate, today.minusDays(3).toDate, "fagstoff")
  val article2 = Article(Some(2), List(ArticleTitle("Pingvinen er ute og går", Some("nb"))), List(ArticleContent("<p>Bilde av en</p><p> en <em>pingvin</em> som vagger borover en gate</p>", None, Some("nb"))), publicDomain, List(ArticleTag(List("fugl"), Some("nb"))), List(), Seq(), Seq(), today.minusDays(4).toDate, today.minusDays(2).toDate, "fagstoff")
  val article3 = Article(Some(3), List(ArticleTitle("Donald Duck kjører bil", Some("nb"))), List(ArticleContent("<p>Bilde av en en and</p><p> som <strong>kjører</strong> en rød bil.</p>", None, Some("nb"))), publicDomain, List(ArticleTag(List("and"), Some("nb"))), List(), Seq(), Seq(), today.minusDays(4).toDate, today.minusDays(1).toDate, "fagstoff")
  val article4 = Article(Some(4), List(ArticleTitle("Superman er ute og flyr", Some("nb"))), List(ArticleContent("<p>Bilde av en flygende mann</p><p> som <strong>har</strong> superkrefter.</p>", None, Some("nb"))), copyrighted, List(ArticleTag(List("supermann"), Some("nb"))), List(), Seq(), Seq(), today.minusDays(4).toDate, today.toDate, "fagstoff")

  override def beforeAll = {
    Path(esDataDir).deleteRecursively()
    val settings = Settings.settingsBuilder()
      .put("path.home", esDataDir)
      .put("index.number_of_shards", "1")
      .put("index.number_of_replicas", "0")
      .put("http.port", esHttpPort)
      .build()

    esNode = new NodeBuilder().settings(settings).node()
    esNode.start()


    val indexName = indexService.createIndex()
    indexService.updateAliasTarget(None, indexName)
    indexService.indexDocuments(List(article1, article2), indexName)
    indexService.indexDocument(article3)
    indexService.indexDocument(article4)

    blockUntil(() => searchService.countDocuments() == 4)
  }

  override def afterAll() = {
    esNode.close()
    Path(esDataDir).deleteRecursively()
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

  test("That all returns all documents ordered by lastUpdated descending") {
    val results = searchService.all(None, None, None, None, Sort.ByLastUpdatedDesc)
    results.totalCount should be (3)
    results.results.head.id should be ("3")
    results.results.last.id should be ("1")
  }

  test("That all returns all documents ordered by lastUpdated ascending") {
    val results = searchService.all(None, None, None, None, Sort.ByLastUpdatedAsc)
    results.totalCount should be (3)
    results.results.head.id should be ("1")
    results.results.last.id should be ("3")
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

  test("That search does not return superman since it has license copyrighted and license is not specified") {
    val results = searchService.matchingQuery(Seq("supermann"), Some("nb"), None, None, None, Sort.ByTitleAsc)
    results.totalCount should be (0)
  }

  test("That search returns superman since license is specified as copyrighted") {
    val results = searchService.matchingQuery(Seq("supermann"), Some("nb"), Some("copyrighted"), None, None, Sort.ByTitleAsc)
    results.totalCount should be (1)
    results.results.head.id should be ("4")
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
