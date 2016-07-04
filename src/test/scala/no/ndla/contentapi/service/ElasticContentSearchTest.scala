package no.ndla.contentapi.service

import com.sksamuel.elastic4s.testkit.ElasticSugar
import no.ndla.contentapi.TestEnvironment
import no.ndla.contentapi.UnitSuite
import no.ndla.contentapi.model._


class ElasticContentSearchTest extends UnitSuite with TestEnvironment with ElasticSugar {

  override val elasticClient = client
  override val elasticContentSearch = new ElasticContentSearch
  override val elasticContentIndex = new ElasticContentIndex

  val byNcSa = Copyright(License("by-nc-sa", "Attribution-NonCommercial-ShareAlike", None), "Gotham City", List(Author("Forfatter", "DC Comics")))
  val publicDomain = Copyright(License("publicdomain", "Public Domain", None), "Metropolis", List(Author("Forfatter", "Bruce Wayne")))

  val content1 = ContentInformation("1", List(ContentTitle("Batmen er på vift med en bil", Some("nb"))), List(Content("Bilde av en <strong>bil</strong> flaggermusmann som vifter med vingene <em>bil</em>.", Map(), Some("nb"))), byNcSa, List(ContentTag("fugl", Some("nb"))), List())
  val content2 = ContentInformation("2", List(ContentTitle("Pingvinen er ute og går", Some("nb"))), List(Content("<p>Bilde av en</p><p> en <em>pingvin</em> som vagger borover en gate</p>", Map(), Some("nb"))), publicDomain, List(ContentTag("fugl", Some("nb"))), List())
  val content3 = ContentInformation("3", List(ContentTitle("Donald Duck kjører bil", Some("nb"))), List(Content("<p>Bilde av en en and</p><p> som <strong>kjører</strong> en rød bil.</p>", Map(), Some("nb"))), publicDomain, List(ContentTag("and", Some("nb"))), List())

  override def beforeAll = {
    val indexName = elasticContentIndex.create()
    elasticContentIndex.updateAliasTarget(None, indexName)
    elasticContentIndex.indexDocuments(List(content1, content2, content3), indexName)

    blockUntilCount(3, indexName)
  }

  test("That getStartAtAndNumResults returns default values for None-input") {
    elasticContentSearch.getStartAtAndNumResults(None, None) should equal((0, DEFAULT_PAGE_SIZE))
  }

  test("That getStartAtAndNumResults returns SEARCH_MAX_PAGE_SIZE for value greater than SEARCH_MAX_PAGE_SIZE") {
    elasticContentSearch.getStartAtAndNumResults(None, Some(1000)) should equal((0, MAX_PAGE_SIZE))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size with default page-size") {
    val page = 74
    val expectedStartAt = (page - 1) * DEFAULT_PAGE_SIZE
    elasticContentSearch.getStartAtAndNumResults(Some(page), None) should equal((expectedStartAt, DEFAULT_PAGE_SIZE))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size") {
    val page = 123
    val pageSize = 321
    val expectedStartAt = (page - 1) * pageSize
    elasticContentSearch.getStartAtAndNumResults(Some(page), Some(pageSize)) should equal((expectedStartAt, pageSize))
  }

  test("That all returns all documents ordered by id ascending") {
    val results = elasticContentSearch.all(None, None, None)
    results.size should be (3)
    results.head.id should be ("1")
    results.last.id should be ("3")
  }

  test("That all filtering on license only returns documents with given license") {
    val results = elasticContentSearch.all(Some("publicdomain"), None, None)
    results.size should be (2)
    results.head.id should be ("2")
    results.last.id should be ("3")
  }

  test("That paging returns only hits on current page and not more than page-size") {
    val page1 = elasticContentSearch.all(None, Some(1), Some(2))
    val page2 = elasticContentSearch.all(None, Some(2), Some(2))
    page1.size should be (2)
    page1.head.id should be ("1")
    page1.last.id should be ("2")
    page2.size should be (1)
    page2.head.id should be ("3")
  }

  test("That search matches title and html-content ordered by relevance") {
    val results = elasticContentSearch.matchingQuery(Seq("bil"), Some("nb"), None, None, None)
    results.size should be (2)
    results.head.id should be ("1")
    results.last.id should be ("3")
  }

  test("That search matches title") {
    val results = elasticContentSearch.matchingQuery(Seq("Pingvinen"), Some("nb"), None, None, None)
    results.size should be (1)
    results.head.id should be ("2")
  }

  test("That search matches tags") {
    val results = elasticContentSearch.matchingQuery(Seq("and"), Some("nb"), None, None, None)
    results.size should be (1)
    results.head.id should be ("3")
  }
}
