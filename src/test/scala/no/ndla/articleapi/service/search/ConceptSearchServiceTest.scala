/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.search

import no.ndla.articleapi.ArticleApiProperties.DefaultPageSize
import no.ndla.articleapi._
import no.ndla.articleapi.integration.JestClientFactory
import no.ndla.articleapi.model.domain._
import no.ndla.tag.IntegrationTest
import org.joda.time.DateTime

@IntegrationTest
class ConceptSearchServiceTest extends UnitSuite with TestEnvironment {

  val esPort = 9200

  override val jestClient = JestClientFactory.getClient(searchServer = s"http://localhost:$esPort")

  override val conceptSearchService = new ConceptSearchService
  override val conceptIndexService = new ConceptIndexService
  override val converterService = new ConverterService
  override val searchConverterService = new SearchConverterService

  val byNcSa = Copyright("by-nc-sa", "Gotham City", List(Author("Forfatter", "DC Comics")))
  val publicDomain = Copyright("publicdomain", "Metropolis", List(Author("Forfatter", "Bruce Wayne")))
  val copyrighted = Copyright("copyrighted", "New York", List(Author("Forfatter", "Clark Kent")))

  val today = DateTime.now()

  val concept1 = TestData.sampleConcept.copy(
    id = Option(1),
    title = List(ConceptTitle("Batmen er på vift med en bil", "nb")),
    content = List(ConceptContent("Bilde av en <strong>bil</strong> flaggermusmann som vifter med vingene <em>bil</em>.", "nb")))
  val concept2 = TestData.sampleConcept.copy(
    id = Option(2),
    title = List(ConceptTitle("Pingvinen er ute og går", "nb")),
    content = List(ConceptContent("<p>Bilde av en</p><p> en <em>pingvin</em> som vagger borover en gate</p>", "nb")))
  val concept3 = TestData.sampleConcept.copy(
    id = Option(3),
    title = List(ConceptTitle("Donald Duck kjører bil", "nb")),
    content = List(ConceptContent("<p>Bilde av en en and</p><p> som <strong>kjører</strong> en rød bil.</p>", "nb")))
  val concept4 = TestData.sampleConcept.copy(
    id = Option(4),
    title = List(ConceptTitle("Superman er ute og flyr", "nb")),
    content = List(ConceptContent("<p>Bilde av en flygende mann</p><p> som <strong>har</strong> superkrefter.</p>", "nb")))
  val concept5 = TestData.sampleConcept.copy(
    id = Option(5),
    title = List(ConceptTitle("Hulken løfter biler", "nb")),
    content = List(ConceptContent("<p>Bilde av hulk</p><p> som <strong>løfter</strong> en rød bil.</p>", "nb")))
  val concept6 = TestData.sampleConcept.copy(
    id = Option(6),
    title = List(ConceptTitle("Loke og Tor prøver å fange midgaardsormen", "nb")),
    content = List(ConceptContent("<p>Bilde av <em>Loke</em> og <em>Tor</em></p><p> som <strong>fisker</strong> fra Naglfar.</p>", "nb")))
  val concept7 = TestData.sampleConcept.copy(
    id = Option(7),
    title = List(ConceptTitle("Yggdrasil livets tre", "nb")),
    content = List(ConceptContent("<p>Bilde av <em>Yggdrasil</em> livets tre med alle dyrene som bor i det.", "nb")))
  val concept8 = TestData.sampleConcept.copy(
    id = Option(8),
    title = List(ConceptTitle("Baldur har mareritt", "nb")),
    content = List(ConceptContent("<p>Bilde av <em>Baldurs</em> mareritt om Ragnarok.", "nb")))
  val concept9 = TestData.sampleConcept.copy(
    id = Option(9),
    title = List(ConceptTitle("Baldur har mareritt om Ragnarok", "nb")),
    content = List(ConceptContent("<p>Bilde av <em>Baldurs</em> som har  mareritt.", "nb")))

  override def beforeAll = {
    conceptIndexService.createIndexWithName(ArticleApiProperties.ArticleSearchIndex)

    conceptIndexService.indexDocument(concept1)
    conceptIndexService.indexDocument(concept2)
    conceptIndexService.indexDocument(concept3)
    conceptIndexService.indexDocument(concept4)
    conceptIndexService.indexDocument(concept5)
    conceptIndexService.indexDocument(concept6)
    conceptIndexService.indexDocument(concept7)
    conceptIndexService.indexDocument(concept8)
    conceptIndexService.indexDocument(concept9)

    blockUntil(() => conceptSearchService.countDocuments == 9)
  }

  override def afterAll() = {
    conceptIndexService.deleteIndex(Some(ArticleApiProperties.ArticleSearchIndex))
  }

  test("That getStartAtAndNumResults returns SEARCH_MAX_PAGE_SIZE for value greater than SEARCH_MAX_PAGE_SIZE") {
    conceptSearchService.getStartAtAndNumResults(0, 1000) should equal((0, ArticleApiProperties.MaxPageSize))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size with default page-size") {
    val page = 74
    val expectedStartAt = (page - 1) * DefaultPageSize
    conceptSearchService.getStartAtAndNumResults(page, DefaultPageSize) should equal((expectedStartAt, DefaultPageSize))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size") {
    val page = 123
    val expectedStartAt = (page - 1) * DefaultPageSize
    conceptSearchService.getStartAtAndNumResults(page, DefaultPageSize) should equal((expectedStartAt, DefaultPageSize))
  }

  test("That all returns all documents ordered by id ascending") {
    val results = conceptSearchService.all(List(), Language.DefaultLanguage, 1, 10, Sort.ByIdAsc)
    val hits = results.results
    results.totalCount should be(9)
    hits.head.id should be(1)
    hits(1).id should be(2)
    hits(2).id should be(3)
    hits(3).id should be(4)
    hits(4).id should be(5)
    hits(5).id should be(6)
    hits(6).id should be(7)
    hits(7).id should be(8)
    hits.last.id should be(9)
  }

  test("That all returns all documents ordered by id descending") {
    val results = conceptSearchService.all(List(), Language.DefaultLanguage, 1, 10, Sort.ByIdDesc)
    val hits = results.results
    results.totalCount should be(9)
    hits.head.id should be (9)
    hits.last.id should be (1)
  }

  test("That all returns all documents ordered by title ascending") {
    val results = conceptSearchService.all(List(), Language.DefaultLanguage, 1, 10, Sort.ByTitleAsc)
    val hits = results.results
    hits.foreach(println)
    results.totalCount should be(9)
    hits.head.id should be(8)
    hits(1).id should be(9)
    hits(2).id should be(1)
    hits(3).id should be(3)
    hits(4).id should be(5)
    hits(5).id should be(6)
    hits(6).id should be(2)
    hits(7).id should be(4)
    hits.last.id should be(7)
  }

  test("That all returns all documents ordered by title descending") {
    val results = conceptSearchService.all(List(), Language.DefaultLanguage, 1, 10, Sort.ByTitleDesc)
    val hits = results.results
    results.totalCount should be(9)
    hits.head.id should be(7)
    hits(1).id should be(4)
    hits(2).id should be(2)
    hits(3).id should be(6)
    hits(4).id should be(5)
    hits(5).id should be(3)
    hits(6).id should be(1)
    hits(7).id should be(9)
    hits.last.id should be(8)

  }

  test("That all filtered by id only returns documents with the given ids") {
    val results = conceptSearchService.all(List(1, 3), Language.DefaultLanguage, 1, 10, Sort.ByIdAsc)
    val hits = results.results
    results.totalCount should be(2)
    hits.head.id should be(1)
    hits.last.id should be(3)
  }

  test("That paging returns only hits on current page and not more than page-size") {
    val page1 = conceptSearchService.all(List(), Language.DefaultLanguage, 1, 2, Sort.ByTitleAsc)
    val page2 = conceptSearchService.all(List(), Language.DefaultLanguage, 2, 2, Sort.ByTitleAsc)

    val hits1 = page1.results
    page1.totalCount should be(9)
    page1.page should be(1)
    hits1.size should be(2)
    hits1.head.id should be(8)
    hits1.last.id should be(9)

    val hits2 = page2.results
    page2.totalCount should be(9)
    page2.page should be(2)
    hits2.size should be(2)
    hits2.head.id should be(1)
    hits2.last.id should be(3)
  }

  test("That search matches title and content ordered by relevance descending") {
    val results = conceptSearchService.matchingQuery("bil", List(), "nb", 1, 10, Sort.ByRelevanceDesc)
    val hits = results.results

    results.totalCount should be(3)
    hits.head.id should be(5)
    hits(1).id should be(1)
    hits.last.id should be(3)
  }

  test("That search matches title") {
    val results = conceptSearchService.matchingQuery("Pingvinen", List(), "nb", 1, 10, Sort.ByTitleAsc)
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(2)
  }

  test("Searching with logical AND only returns results with all terms") {
    val search1 = conceptSearchService.matchingQuery("bilde + bil", List(), "nb", 1, 10, Sort.ByTitleAsc)
    val hits1 = search1.results
    hits1.map(_.id) should equal (Seq(1, 3, 5))

    val search2 = conceptSearchService.matchingQuery("batmen + bil", List(), "nb", 1, 10, Sort.ByTitleAsc)
    val hits2 = search2.results
    hits2.map(_.id) should equal (Seq(1))

    val search3 = conceptSearchService.matchingQuery("bil + bilde + -flaggermusmann", List(), "nb", 1, 10, Sort.ByTitleAsc)
    val hits3 = search3.results
    hits3.map(_.id) should equal (Seq(3, 5))

    val search4 = conceptSearchService.matchingQuery("bil + -hulken", List(), "nb", 1, 10, Sort.ByTitleAsc)
    val hits4 = search4.results
    hits4.map(_.id) should equal (Seq(1, 3))
  }

  test("search in content should be ranked lower than title") {
    val search = conceptSearchService.matchingQuery("mareritt + ragnarok", List(), "nb", 1, 10, Sort.ByRelevanceDesc)
    val hits = search.results
    hits.map(_.id) should equal (Seq(9, 8))
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
