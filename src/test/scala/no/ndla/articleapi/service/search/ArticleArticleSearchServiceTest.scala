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

@IntegrationTest
class ArticleSearchServiceTest extends UnitSuite with TestEnvironment {

  val esPort = 9200

  override val jestClient = JestClientFactory.getClient(searchServer = s"http://localhost:$esPort")

  override val articleSearchService = new ArticleSearchService
  override val articleIndexService = new ArticleIndexService
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
  val article5 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(5),
    title = List(ArticleTitle("Hulken løfter biler", Some("nb"))),
    introduction = List(ArticleIntroduction("Hulken", Some("nb"))),
    content = List(ArticleContent("<p>Bilde av hulk</p><p> som <strong>løfter</strong> en rød bil.</p>", None, Some("nb"))),
    tags = List(ArticleTag(List("hulk"), Some("nb"))),
    created = today.minusDays(40).toDate,
    updated = today.minusDays(35).toDate
  )
  val article6 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(6),
    title = List(ArticleTitle("Loke og Tor prøver å fange midgaardsormen", Some("nb"))),
    introduction = List(ArticleIntroduction("Loke og Tor", Some("nb"))),
    content = List(ArticleContent("<p>Bilde av <em>Loke</em> og <em>Tor</em></p><p> som <strong>fisker</strong> fra Naglfar.</p>", None, Some("nb"))),
    tags = List(ArticleTag(List("Loke", "Tor", "Naglfar"), Some("nb"))),
    created = today.minusDays(30).toDate,
    updated = today.minusDays(25).toDate
  )
  val article7 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(7),
    title = List(ArticleTitle("Yggdrasil livets tre", Some("nb"))),
    introduction = List(ArticleIntroduction("Yggdrasil", Some("nb"))),
    content = List(ArticleContent("<p>Bilde av <em>Yggdrasil</em> livets tre med alle dyrene som bor i det.", None, Some("nb"))),
    tags = List(ArticleTag(List("yggdrasil"), Some("nb"))),
    created = today.minusDays(20).toDate,
    updated = today.minusDays(15).toDate
  )
  val article8 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(8),
    title = List(ArticleTitle("Baldur har mareritt", Some("nb"))),
    introduction = List(ArticleIntroduction("Baldur", Some("nb"))),
    content = List(ArticleContent("<p>Bilde av <em>Baldurs</em> mareritt om Ragnarok.", None, Some("nb"))),
    tags = List(ArticleTag(List("baldur"), Some("nb"))),
    created = today.minusDays(10).toDate,
    updated = today.minusDays(5).toDate,
    articleType = ArticleType.TopicArticle.toString
  )
  val article9 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(9),
    title = List(ArticleTitle("Baldur har mareritt om Ragnarok", Some("nb"))),
    introduction = List(ArticleIntroduction("Baldur", Some("nb"))),
    content = List(ArticleContent("<p>Bilde av <em>Baldurs</em> som har  mareritt.", None, Some("nb"))),
    tags = List(ArticleTag(List("baldur"), Some("nb"))),
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
    results.results.head.id should be("8")

    val results2 = articleSearchService.all(List(), Language.DefaultLanguage, None, 1, 10, Sort.ByIdAsc, ArticleType.all)
    results2.totalCount should be(7)
  }

  test("That all returns all documents ordered by id ascending") {
    val results = articleSearchService.all(List(), Language.DefaultLanguage, None, 1, 10, Sort.ByIdAsc, Seq.empty)
    results.totalCount should be(7)
    results.results.head.id should be("1")
    results.results(1).id should be("2")
    results.results(2).id should be("3")
    results.results(3).id should be("5")
    results.results(4).id should be("6")
    results.results(5).id should be("7")
    results.results.last.id should be("8")
  }

  test("That all returns all documents ordered by id descending") {
    val results = articleSearchService.all(List(), Language.DefaultLanguage, None, 1, 10, Sort.ByIdDesc, Seq.empty)
    results.totalCount should be(7)
    results.results.head.id should be("8")
    results.results.last.id should be("1")
  }

  test("That all returns all documents ordered by title ascending") {
    val results = articleSearchService.all(List(), Language.DefaultLanguage, None, 1, 10, Sort.ByTitleAsc, Seq.empty)
    results.totalCount should be(7)
    results.results.head.id should be("8")
    results.results(1).id should be("1")
    results.results(2).id should be("3")
    results.results(3).id should be("5")
    results.results(4).id should be("6")
    results.results(5).id should be("2")
    results.results.last.id should be("7")
  }

  test("That all returns all documents ordered by title descending") {
    val results = articleSearchService.all(List(), Language.DefaultLanguage, None, 1, 10, Sort.ByTitleDesc, Seq.empty)
    results.totalCount should be(7)
    results.results.head.id should be("7")
    results.results(1).id should be("2")
    results.results(2).id should be("6")
    results.results(3).id should be("5")
    results.results(4).id should be("3")
    results.results(5).id should be("1")
    results.results.last.id should be("8")

  }

  test("That all returns all documents ordered by lastUpdated descending") {
    val results = articleSearchService.all(List(), Language.DefaultLanguage, None, 1, 10, Sort.ByLastUpdatedDesc, Seq.empty)
    results.totalCount should be(7)

    results.results.head.id should be("3")
    results.results.last.id should be("5")
  }

  test("That all returns all documents ordered by lastUpdated ascending") {
    val results = articleSearchService.all(List(), Language.DefaultLanguage, None, 1, 10, Sort.ByLastUpdatedAsc, Seq.empty)
    results.totalCount should be(7)
    results.results.head.id should be("5")
    results.results(1).id should be("6")
    results.results(2).id should be("7")
    results.results(3).id should be("8")
    results.results(4).id should be("1")
    results.results(5).id should be("2")
    results.results.last.id should be("3")
  }

  test("That all filtering on license only returns documents with given license") {
    val results = articleSearchService.all(List(), Language.DefaultLanguage, Some("publicdomain"), 1, 10, Sort.ByTitleAsc, Seq.empty)
    results.totalCount should be(6)
    results.results.head.id should be("8")
    results.results(1).id should be("3")
    results.results(2).id should be("5")
    results.results(3).id should be("6")
    results.results(4).id should be("2")
    results.results.last.id should be("7")
  }

  test("That all filtered by id only returns documents with the given ids") {
    val results = articleSearchService.all(List(1, 3), Language.DefaultLanguage, None, 1, 10, Sort.ByIdAsc, Seq.empty)
    results.totalCount should be(2)
    results.results.head.id should be("1")
    results.results.last.id should be("3")
  }

  test("That paging returns only hits on current page and not more than page-size") {
    val page1 = articleSearchService.all(List(), Language.DefaultLanguage, None, 1, 2, Sort.ByTitleAsc, Seq.empty)
    val page2 = articleSearchService.all(List(), Language.DefaultLanguage, None, 2, 2, Sort.ByTitleAsc, Seq.empty)
    page1.totalCount should be(7)
    page1.page should be(1)
    page1.results.size should be(2)
    page1.results.head.id should be("8")
    page1.results.last.id should be("1")
    page2.totalCount should be(7)
    page2.page should be(2)
    page2.results.size should be(2)
    page2.results.head.id should be("3")
    page2.results.last.id should be("5")
  }

  test("mathcingQuery should filter results based on an article type filter") {
    val results = articleSearchService.matchingQuery("bil", List(), "nb", None, 1, 10, Sort.ByRelevanceDesc, Seq(ArticleType.TopicArticle.toString))
    results.totalCount should be(0)

    val results2 = articleSearchService.matchingQuery("bil", List(), "nb", None, 1, 10, Sort.ByRelevanceDesc, Seq(ArticleType.Standard.toString))
    results2.totalCount should be(3)
  }

  test("That search matches title and html-content ordered by relevance descending") {
    val results = articleSearchService.matchingQuery("bil", List(), "nb", None, 1, 10, Sort.ByRelevanceDesc, Seq.empty)

    results.totalCount should be(3)
    results.results.head.id should be("1")
    results.results(1).id should be("5")
    results.results.last.id should be("3")
  }

  test("That search combined with filter by id only returns documents matching the query with one of the given ids") {
    val results = articleSearchService.matchingQuery("bil", List(3), "nb", None, 1, 10, Sort.ByRelevanceDesc, Seq.empty)
    results.totalCount should be(1)
    results.results.head.id should be("3")
    results.results.last.id should be("3")
  }

  test("That search matches title") {
    val results = articleSearchService.matchingQuery("Pingvinen", List(), "nb", None, 1, 10, Sort.ByTitleAsc, Seq.empty)
    results.totalCount should be(1)
    results.results.head.id should be("2")
  }

  test("That search matches tags") {
    val results = articleSearchService.matchingQuery("and", List(), "nb", None, 1, 10, Sort.ByTitleAsc, Seq.empty)
    results.totalCount should be(1)
    results.results.head.id should be("3")
  }

  test("That search does not return superman since it has license copyrighted and license is not specified") {
    val results = articleSearchService.matchingQuery("supermann", List(), "nb", None, 1, 10, Sort.ByTitleAsc, Seq.empty)
    results.totalCount should be(0)
  }

  test("That search returns superman since license is specified as copyrighted") {
    val results = articleSearchService.matchingQuery("supermann", List(), "nb", Some("copyrighted"), 1, 10, Sort.ByTitleAsc, Seq.empty)
    results.totalCount should be(1)
    results.results.head.id should be("4")
  }

  test("Searching with logical AND only returns results with all terms") {
    val search1 = articleSearchService.matchingQuery("bilde AND bil", List(), "nb", None, 1, 10, Sort.ByTitleAsc, Seq.empty)
    search1.results.map(_.id) should equal (Seq("1", "3", "5"))

    val search2 = articleSearchService.matchingQuery("batmen AND bil", List(), "nb", None, 1, 10, Sort.ByTitleAsc, Seq.empty)
    search2.results.map(_.id) should equal (Seq("1"))

    val search3 = articleSearchService.matchingQuery("bil AND bilde AND NOT flaggermusmann", List(), "nb", None, 1, 10, Sort.ByTitleAsc, Seq.empty)
    search3.results.map(_.id) should equal (Seq("3", "5"))

    val search4 = articleSearchService.matchingQuery("bil AND NOT hulken", List(), "nb", None, 1, 10, Sort.ByTitleAsc, Seq.empty)
    search4.results.map(_.id) should equal (Seq("1", "3"))
  }

  test("search in content should be ranked lower than introduction and title") {
    val search = articleSearchService.matchingQuery("mareritt + ragnarok", List(), "nb", None, 1, 10, Sort.ByRelevanceDesc, Seq.empty)
    search.results.map(_.id) should equal (Seq("9", "8"))
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
