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
import no.ndla.articleapi.ArticleApiProperties.DefaultPageSize
import no.ndla.tag.IntegrationTest
import org.joda.time.DateTime

import scala.util.parsing.json.JSONObject

@IntegrationTest
class ArticleSearchServiceTest extends UnitSuite with TestEnvironment {

  val esPort = 9200

  override val jestClient = JestClientFactory.getClient(searchServer = s"http://localhost:$esPort")

  override val articleSearchService = new ArticleSearchService
  override val articleIndexService = new ArticleIndexService
  override val converterService = new ConverterService
  override val searchConverterService = new SearchConverterService

  val byNcSa = Copyright("by-nc-sa", "Gotham City", List(Author("Forfatter", "DC Comics")))
  val publicDomain = Copyright("publicdomain", "Metropolis", List(Author("Forfatter", "Bruce Wayne")))
  val copyrighted = Copyright("copyrighted", "New York", List(Author("Forfatter", "Clark Kent")))

  val today = DateTime.now()

  val article1 = TestData.sampleArticleWithByNcSa.copy(
    id = Option(1),
    title = List(ArticleTitle("Batmen er på vift med en bil", "nb")),
    introduction = List(ArticleIntroduction("Batmen", "nb")),
    content = List(ArticleContent("Bilde av en <strong>bil</strong> flaggermusmann som vifter med vingene <em>bil</em>.", None, "nb")),
    tags = List(ArticleTag(List("fugl"), "nb")),
    created = today.minusDays(4).toDate,
    updated = today.minusDays(3).toDate)
  val article2 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(2),
    title = List(ArticleTitle("Pingvinen er ute og går", "nb")),
    introduction = List(ArticleIntroduction("Pingvinen", "nb")),
    content = List(ArticleContent("<p>Bilde av en</p><p> en <em>pingvin</em> som vagger borover en gate</p>", None, "nb")),
    tags = List(ArticleTag(List("fugl"), "nb")),
    created = today.minusDays(4).toDate,
    updated = today.minusDays(2).toDate)
  val article3 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(3),
    title = List(ArticleTitle("Donald Duck kjører bil", "nb")),
    introduction = List(ArticleIntroduction("Donald Duck", "nb")),
    content = List(ArticleContent("<p>Bilde av en en and</p><p> som <strong>kjører</strong> en rød bil.</p>", None, "nb")),
    tags = List(ArticleTag(List("and"), "nb")),
    created = today.minusDays(4).toDate,
    updated = today.minusDays(1).toDate
  )
  val article4 = TestData.sampleArticleWithCopyrighted.copy(
    id = Option(4),
    title = List(ArticleTitle("Superman er ute og flyr", "nb")),
    introduction = List(ArticleIntroduction("Superman", "nb")),
    content = List(ArticleContent("<p>Bilde av en flygende mann</p><p> som <strong>har</strong> superkrefter.</p>", None, "nb")),
    tags = List(ArticleTag(List("supermann"), "nb")),
    created = today.minusDays(4).toDate,
    updated = today.toDate
  )
  val article5 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(5),
    title = List(ArticleTitle("Hulken løfter biler", "nb")),
    introduction = List(ArticleIntroduction("Hulken", "nb")),
    content = List(ArticleContent("<p>Bilde av hulk</p><p> som <strong>løfter</strong> en rød bil.</p>", None, "nb")),
    tags = List(ArticleTag(List("hulk"), "nb")),
    created = today.minusDays(40).toDate,
    updated = today.minusDays(35).toDate
  )
  val article6 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(6),
    title = List(ArticleTitle("Loke og Tor prøver å fange midgaardsormen", "nb")),
    introduction = List(ArticleIntroduction("Loke og Tor", "nb")),
    content = List(ArticleContent("<p>Bilde av <em>Loke</em> og <em>Tor</em></p><p> som <strong>fisker</strong> fra Naglfar.</p>", None, "nb")),
    tags = List(ArticleTag(List("Loke", "Tor", "Naglfar"), "nb")),
    created = today.minusDays(30).toDate,
    updated = today.minusDays(25).toDate
  )
  val article7 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(7),
    title = List(ArticleTitle("Yggdrasil livets tre", "nb")),
    introduction = List(ArticleIntroduction("Yggdrasil", "nb")),
    content = List(ArticleContent("<p>Bilde av <em>Yggdrasil</em> livets tre med alle dyrene som bor i det.", None, "nb")),
    tags = List(ArticleTag(List("yggdrasil"), "nb")),
    created = today.minusDays(20).toDate,
    updated = today.minusDays(15).toDate
  )
  val article8 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(8),
    title = List(ArticleTitle("Baldur har mareritt", "nb")),
    introduction = List(ArticleIntroduction("Baldur", "nb")),
    content = List(ArticleContent("<p>Bilde av <em>Baldurs</em> mareritt om Ragnarok.", None, "nb")),
    tags = List(ArticleTag(List("baldur"), "nb")),
    created = today.minusDays(10).toDate,
    updated = today.minusDays(5).toDate,
    articleType = ArticleType.TopicArticle.toString
  )
  val article9 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(9),
    title = List(ArticleTitle("Baldur har mareritt om Ragnarok", "nb")),
    introduction = List(ArticleIntroduction("Baldur", "nb")),
    content = List(ArticleContent("<p>Bilde av <em>Baldurs</em> som har  mareritt.", None, "nb")),
    tags = List(ArticleTag(List("baldur"), "nb")),
    created = today.minusDays(10).toDate,
    updated = today.minusDays(5).toDate,
    articleType = ArticleType.TopicArticle.toString
  )

  override def beforeAll = {
    articleIndexService.createIndexWithName(ArticleApiProperties.ArticleSearchIndex)

    articleIndexService.indexDocument(article1)
    articleIndexService.indexDocument(article2)
    articleIndexService.indexDocument(article3)
    articleIndexService.indexDocument(article4)
    articleIndexService.indexDocument(article5)
    articleIndexService.indexDocument(article6)
    articleIndexService.indexDocument(article7)
    articleIndexService.indexDocument(article8)
    articleIndexService.indexDocument(article9)

    blockUntil(() => articleSearchService.countDocuments == 9)
  }

  override def afterAll() = {
    articleIndexService.deleteIndex(Some(ArticleApiProperties.ArticleSearchIndex))
  }

  test("That getStartAtAndNumResults returns SEARCH_MAX_PAGE_SIZE for value greater than SEARCH_MAX_PAGE_SIZE") {
    articleSearchService.getStartAtAndNumResults(0, 1000) should equal((0, ArticleApiProperties.MaxPageSize))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size with default page-size") {
    val page = 74
    val expectedStartAt = (page - 1) * DefaultPageSize
    articleSearchService.getStartAtAndNumResults(page, DefaultPageSize) should equal((expectedStartAt, DefaultPageSize))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size") {
    val page = 123
    val expectedStartAt = (page - 1) * DefaultPageSize
    articleSearchService.getStartAtAndNumResults(page, DefaultPageSize) should equal((expectedStartAt, DefaultPageSize))
  }

  test("all should return only articles of a given type if a type filter is specified") {
    val results = articleSearchService.all(List(), Language.DefaultLanguage, None, 1, 10, Sort.ByIdAsc, Seq(ArticleType.TopicArticle.toString))
    results.totalCount should be(1)
    converterService.getHits(results.response).head.id should be ("8")

    val results2 = articleSearchService.all(List(), Language.DefaultLanguage, None, 1, 10, Sort.ByIdAsc, ArticleType.all)
    results2.totalCount should be(7)
  }

  test("That all returns all documents ordered by id ascending") {
    val results = articleSearchService.all(List(), Language.DefaultLanguage, None, 1, 10, Sort.ByIdAsc, Seq.empty)
    val hits = converterService.getHits(results.response)
    results.totalCount should be(7)
    hits.head.id should be("1")
    hits(1).id should be("2")
    hits(2).id should be("3")
    hits(3).id should be("5")
    hits(4).id should be("6")
    hits(5).id should be("7")
    hits.last.id should be("8")
  }

  test("That all returns all documents ordered by id descending") {
    val results = articleSearchService.all(List(), Language.DefaultLanguage, None, 1, 10, Sort.ByIdDesc, Seq.empty)
    val hits = converterService.getHits(results.response)
    results.totalCount should be(7)
    hits.head.id should be ("8")
    hits.last.id should be ("1")
  }

  test("That all returns all documents ordered by title ascending") {
    val results = articleSearchService.all(List(), Language.DefaultLanguage, None, 1, 10, Sort.ByTitleAsc, Seq.empty)
    val hits = converterService.getHits(results.response)
    results.totalCount should be(7)
    hits.head.id should be("8")
    hits(1).id should be("1")
    hits(2).id should be("3")
    hits(3).id should be("5")
    hits(4).id should be("6")
    hits(5).id should be("2")
    hits.last.id should be("7")
  }

  test("That all returns all documents ordered by title descending") {
    val results = articleSearchService.all(List(), Language.DefaultLanguage, None, 1, 10, Sort.ByTitleDesc, Seq.empty)
    val hits = converterService.getHits(results.response)
    results.totalCount should be(7)
    hits.head.id should be("7")
    hits(1).id should be("2")
    hits(2).id should be("6")
    hits(3).id should be("5")
    hits(4).id should be("3")
    hits(5).id should be("1")
    hits.last.id should be("8")

  }

  test("That all returns all documents ordered by lastUpdated descending") {
    val results = articleSearchService.all(List(), Language.DefaultLanguage, None, 1, 10, Sort.ByLastUpdatedDesc, Seq.empty)
    val hits = converterService.getHits(results.response)
    results.totalCount should be(7)
    hits.head.id should be("3")
    hits.last.id should be("5")
  }

  test("That all returns all documents ordered by lastUpdated ascending") {
    val results = articleSearchService.all(List(), Language.DefaultLanguage, None, 1, 10, Sort.ByLastUpdatedAsc, Seq.empty)
    val hits = converterService.getHits(results.response)
    results.totalCount should be(7)
    hits.head.id should be("5")
    hits(1).id should be("6")
    hits(2).id should be("7")
    hits(3).id should be("8")
    hits(4).id should be("1")
    hits(5).id should be("2")
    hits.last.id should be("3")
  }

  test("That all filtering on license only returns documents with given license") {
    val results = articleSearchService.all(List(), Language.DefaultLanguage, Some("publicdomain"), 1, 10, Sort.ByTitleAsc, Seq.empty)
    val hits = converterService.getHits(results.response)
    results.totalCount should be(6)
    hits.head.id should be("8")
    hits(1).id should be("3")
    hits(2).id should be("5")
    hits(3).id should be("6")
    hits(4).id should be("2")
    hits.last.id should be("7")
  }

  test("That all filtered by id only returns documents with the given ids") {
    val results = articleSearchService.all(List(1, 3), Language.DefaultLanguage, None, 1, 10, Sort.ByIdAsc, Seq.empty)
    val hits = converterService.getHits(results.response)
    results.totalCount should be(2)
    hits.head.id should be("1")
    hits.last.id should be("3")
  }

  test("That paging returns only hits on current page and not more than page-size") {
    val page1 = articleSearchService.all(List(), Language.DefaultLanguage, None, 1, 2, Sort.ByTitleAsc, Seq.empty)
    val page2 = articleSearchService.all(List(), Language.DefaultLanguage, None, 2, 2, Sort.ByTitleAsc, Seq.empty)
    val hits1 = converterService.getHits(page1.response)
    val hits2 = converterService.getHits(page1.response)
    page1.totalCount should be(7)
    page1.page should be(1)
    hits1.size should be(2)
    hits1.head.id should be("8")
    hits1.last.id should be("1")
    page2.totalCount should be(7)
    page2.page should be(2)
    hits2.size should be(2)
    hits2.head.id should be("3")
    hits2.last.id should be("5")
  }

  test("mathcingQuery should filter results based on an article type filter") {
    val results = articleSearchService.matchingQuery("bil", List(), "nb", None, 1, 10, Sort.ByRelevanceDesc, Seq(ArticleType.TopicArticle.toString))
    results.totalCount should be(0)

    val results2 = articleSearchService.matchingQuery("bil", List(), "nb", None, 1, 10, Sort.ByRelevanceDesc, Seq(ArticleType.Standard.toString))
    results2.totalCount should be(3)
  }

  test("That search matches title and html-content ordered by relevance descending") {
    val results = articleSearchService.matchingQuery("bil", List(), "nb", None, 1, 10, Sort.ByRelevanceDesc, Seq.empty)
    val hits = converterService.getHits(results.response)
    results.totalCount should be(3)
    hits.head.id should be("1")
    hits(1).id should be("5")
    hits.last.id should be("3")
  }

  test("That search combined with filter by id only returns documents matching the query with one of the given ids") {
    val results = articleSearchService.matchingQuery("bil", List(3), "nb", None, 1, 10, Sort.ByRelevanceDesc, Seq.empty)
    val hits = converterService.getHits(results.response)
    results.totalCount should be(1)
    hits.head.id should be("3")
    hits.last.id should be("3")
  }

  test("That search matches title") {
    val results = articleSearchService.matchingQuery("Pingvinen", List(), "nb", None, 1, 10, Sort.ByTitleAsc, Seq.empty)
    val hits = converterService.getHits(results.response)
    results.totalCount should be(1)
    hits.head.id should be("2")
  }

  test("That search matches tags") {
    val results = articleSearchService.matchingQuery("and", List(), "nb", None, 1, 10, Sort.ByTitleAsc, Seq.empty)
    val hits = converterService.getHits(results.response)
    results.totalCount should be(1)
    hits.head.id should be("3")
  }

  test("That search does not return superman since it has license copyrighted and license is not specified") {
    val results = articleSearchService.matchingQuery("supermann", List(), "nb", None, 1, 10, Sort.ByTitleAsc, Seq.empty)
    results.totalCount should be(0)
  }

  test("That search returns superman since license is specified as copyrighted") {
    val results = articleSearchService.matchingQuery("supermann", List(), "nb", Some("copyrighted"), 1, 10, Sort.ByTitleAsc, Seq.empty)
    val hits = converterService.getHits(results.response)
    results.totalCount should be(1)
    hits.head.id should be("4")
  }

  test("Searching with logical AND only returns results with all terms") {
    val search1 = articleSearchService.matchingQuery("bilde AND bil", List(), "nb", None, 1, 10, Sort.ByTitleAsc, Seq.empty)
    val hits1 = converterService.getHits(search1.response)
    hits1.map(_.id) should equal (Seq("1", "3", "5"))

    val search2 = articleSearchService.matchingQuery("batmen AND bil", List(), "nb", None, 1, 10, Sort.ByTitleAsc, Seq.empty)
    val hits2 = converterService.getHits(search1.response)
    hits2.map(_.id) should equal (Seq("1"))

    val search3 = articleSearchService.matchingQuery("bil AND bilde AND NOT flaggermusmann", List(), "nb", None, 1, 10, Sort.ByTitleAsc, Seq.empty)
    val hits3 = converterService.getHits(search1.response)
    hits3.map(_.id) should equal (Seq("3", "5"))

    val search4 = articleSearchService.matchingQuery("bil AND NOT hulken", List(), "nb", None, 1, 10, Sort.ByTitleAsc, Seq.empty)
    val hits4 = converterService.getHits(search1.response)
    hits4.map(_.id) should equal (Seq("1", "3"))
  }

  test("search in content should be ranked lower than introduction and title") {
    val search = articleSearchService.matchingQuery("mareritt + ragnarok", List(), "nb", None, 1, 10, Sort.ByRelevanceDesc, Seq.empty)
    val hits = converterService.getHits(search.response)
    hits.map(_.id) should equal (Seq("9", "8"))
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
