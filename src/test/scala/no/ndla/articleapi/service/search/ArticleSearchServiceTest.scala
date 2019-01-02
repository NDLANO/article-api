/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.search

import java.nio.file.{Files, Path}

import com.sksamuel.elastic4s.embedded.LocalNode
import no.ndla.articleapi.integration.{Elastic4sClientFactory, NdlaE4sClient}
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi._
import no.ndla.articleapi.model.api
import no.ndla.articleapi.ArticleApiProperties.DefaultPageSize
import no.ndla.mapping.License.{CC_BY_NC_SA, Copyrighted, PublicDomain}
import org.joda.time.DateTime

import scala.util.Success

class ArticleSearchServiceTest extends UnitSuite with TestEnvironment {
  val tmpDir: Path = Files.createTempDirectory(this.getClass.getName)
  val localNodeSettings: Map[String, String] = LocalNode.requiredSettings(this.getClass.getName, tmpDir.toString)
  val localNode = LocalNode(localNodeSettings)

  override val e4sClient = NdlaE4sClient(localNode.client(true))

  override val articleSearchService = new ArticleSearchService
  override val articleIndexService = new ArticleIndexService
  override val converterService = new ConverterService
  override val searchConverterService = new SearchConverterService

  val byNcSa =
    Copyright(CC_BY_NC_SA.toString,
              "Gotham City",
              List(Author("Forfatter", "DC Comics")),
              List(),
              List(),
              None,
              None,
              None)

  val publicDomain =
    Copyright(PublicDomain.toString,
              "Metropolis",
              List(Author("Forfatter", "Bruce Wayne")),
              List(),
              List(),
              None,
              None,
              None)

  val copyrighted =
    Copyright(Copyrighted.toString,
              "New York",
              List(Author("Forfatter", "Clark Kent")),
              List(),
              List(),
              None,
              None,
              None)

  val today = DateTime.now()

  val article1 = TestData.sampleArticleWithByNcSa.copy(
    id = Option(1),
    title = List(ArticleTitle("Batmen er på vift med en bil", "nb")),
    introduction = List(ArticleIntroduction("Batmen", "nb")),
    content = List(
      ArticleContent("Bilde av en <strong>bil</strong> flaggermusmann som vifter med vingene <em>bil</em>.", "nb")),
    tags = List(ArticleTag(List("fugl"), "nb")),
    created = today.minusDays(4).toDate,
    updated = today.minusDays(3).toDate,
    metaImage = List(ArticleMetaImage("5555", "Alt text is here friend", "nb"))
  )

  val article2 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(2),
    title = List(ArticleTitle("Pingvinen er ute og går", "nb")),
    introduction = List(ArticleIntroduction("Pingvinen", "nb")),
    content = List(ArticleContent("<p>Bilde av en</p><p> en <em>pingvin</em> som vagger borover en gate</p>", "nb")),
    tags = List(ArticleTag(List("fugl"), "nb")),
    created = today.minusDays(4).toDate,
    updated = today.minusDays(2).toDate
  )

  val article3 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(3),
    title = List(ArticleTitle("Donald Duck kjører bil", "nb")),
    introduction = List(ArticleIntroduction("Donald Duck", "nb")),
    content = List(ArticleContent("<p>Bilde av en en and</p><p> som <strong>kjører</strong> en rød bil.</p>", "nb")),
    tags = List(ArticleTag(List("and"), "nb")),
    created = today.minusDays(4).toDate,
    updated = today.minusDays(1).toDate
  )

  val article4 = TestData.sampleArticleWithCopyrighted.copy(
    id = Option(4),
    title = List(ArticleTitle("Superman er ute og flyr", "nb")),
    introduction = List(ArticleIntroduction("Superman", "nb")),
    content =
      List(ArticleContent("<p>Bilde av en flygende mann</p><p> som <strong>har</strong> superkrefter.</p>", "nb")),
    tags = List(ArticleTag(List("supermann"), "nb")),
    created = today.minusDays(4).toDate,
    updated = today.toDate
  )

  val article5 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(5),
    title = List(ArticleTitle("Hulken løfter biler", "nb")),
    introduction = List(ArticleIntroduction("Hulken", "nb")),
    content = List(ArticleContent("<p>Bilde av hulk</p><p> som <strong>løfter</strong> en rød bil.</p>", "nb")),
    tags = List(ArticleTag(List("hulk"), "nb")),
    created = today.minusDays(40).toDate,
    updated = today.minusDays(35).toDate
  )

  val article6 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(6),
    title = List(ArticleTitle("Loke og Tor prøver å fange midgaardsormen", "nb")),
    introduction = List(ArticleIntroduction("Loke og Tor", "nb")),
    content = List(
      ArticleContent("<p>Bilde av <em>Loke</em> og <em>Tor</em></p><p> som <strong>fisker</strong> fra Naglfar.</p>",
                     "nb")),
    tags = List(ArticleTag(List("Loke", "Tor", "Naglfar"), "nb")),
    created = today.minusDays(30).toDate,
    updated = today.minusDays(25).toDate
  )

  val article7 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(7),
    title = List(ArticleTitle("Yggdrasil livets tre", "nb")),
    introduction = List(ArticleIntroduction("Yggdrasil", "nb")),
    content = List(ArticleContent("<p>Bilde av <em>Yggdrasil</em> livets tre med alle dyrene som bor i det.", "nb")),
    tags = List(ArticleTag(List("yggdrasil"), "nb")),
    created = today.minusDays(20).toDate,
    updated = today.minusDays(15).toDate
  )

  val article8 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(8),
    title = List(ArticleTitle("Baldur har mareritt", "nb")),
    introduction = List(ArticleIntroduction("Baldur", "nb")),
    content = List(ArticleContent("<p>Bilde av <em>Baldurs</em> mareritt om Ragnarok.", "nb")),
    tags = List(ArticleTag(List("baldur"), "nb")),
    created = today.minusDays(10).toDate,
    updated = today.minusDays(5).toDate,
    articleType = ArticleType.TopicArticle.toString
  )

  val article9 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(9),
    title = List(ArticleTitle("En Baldur har mareritt om Ragnarok", "nb")),
    introduction = List(ArticleIntroduction("Baldur", "nb")),
    content = List(ArticleContent("<p>Bilde av <em>Baldurs</em> som har  mareritt.", "nb")),
    tags = List(ArticleTag(List("baldur"), "nb")),
    created = today.minusDays(10).toDate,
    updated = today.minusDays(5).toDate,
    articleType = ArticleType.TopicArticle.toString
  )

  val article10 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(10),
    title = List(ArticleTitle("This article is in english", "en")),
    introduction = List(ArticleIntroduction("Engulsk", "en")),
    content = List(ArticleContent("<p>Something something <em>english</em> What about", "en")),
    tags = List(ArticleTag(List("englando"), "en")),
    created = today.minusDays(10).toDate,
    updated = today.minusDays(5).toDate,
    articleType = ArticleType.TopicArticle.toString
  )

  val article11 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(11),
    title = List(ArticleTitle("Katter", "nb"), ArticleTitle("Cats", "en")),
    introduction = List(ArticleIntroduction("Katter er store", "nb"), ArticleIntroduction("Cats are big", "en")),
    metaDescription = List(ArticleMetaDescription("hurr durr ima sheep", "en")),
    content = List(ArticleContent("<p>Noe om en katt</p>", "nb"), ArticleContent("<p>Something about a cat</p>", "en")),
    tags = List(ArticleTag(List("ikkehund"), "nb"), ArticleTag(List("notdog"), "en")),
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
    articleIndexService.indexDocument(article10)
    articleIndexService.indexDocument(article11)

    blockUntil(() => articleSearchService.countDocuments == 11)
  }

  override def afterAll() = {
    articleIndexService.deleteIndexWithName(Some(ArticleApiProperties.ArticleSearchIndex))
  }

  test("That getStartAtAndNumResults returns SEARCH_MAX_PAGE_SIZE for value greater than SEARCH_MAX_PAGE_SIZE") {
    articleSearchService.getStartAtAndNumResults(0, 1000) should equal((0, ArticleApiProperties.MaxPageSize))
  }

  test(
    "That getStartAtAndNumResults returns the correct calculated start at for page and page-size with default page-size") {
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
    val Success(results) = articleSearchService.all(List(),
                                                    Language.DefaultLanguage,
                                                    None,
                                                    1,
                                                    10,
                                                    Sort.ByIdAsc,
                                                    Seq(ArticleType.TopicArticle.toString),
                                                    fallback = false)
    results.totalCount should be(3)

    val Success(results2) = articleSearchService.all(List(),
                                                     Language.DefaultLanguage,
                                                     None,
                                                     1,
                                                     10,
                                                     Sort.ByIdAsc,
                                                     ArticleType.all,
                                                     fallback = false)
    results2.totalCount should be(9)
  }

  test("That all returns all documents ordered by id ascending") {
    val Success(results) =
      articleSearchService.all(List(), Language.DefaultLanguage, None, 1, 10, Sort.ByIdAsc, Seq.empty, fallback = false)
    val hits = results.results
    results.totalCount should be(9)
    hits.head.id should be(1)
    hits(1).id should be(2)
    hits(2).id should be(3)
    hits(3).id should be(5)
    hits(4).id should be(6)
    hits(5).id should be(7)
    hits.last.id should be(11)
  }

  test("That all returns all documents ordered by id descending") {
    val Success(results) = articleSearchService.all(List(),
                                                    Language.DefaultLanguage,
                                                    None,
                                                    1,
                                                    10,
                                                    Sort.ByIdDesc,
                                                    Seq.empty,
                                                    fallback = false)
    val hits = results.results
    results.totalCount should be(9)
    hits.head.id should be(11)
    hits.last.id should be(1)
  }

  test("That all returns all documents ordered by title ascending") {
    val Success(results) = articleSearchService.all(List(),
                                                    Language.DefaultLanguage,
                                                    None,
                                                    1,
                                                    10,
                                                    Sort.ByTitleAsc,
                                                    Seq.empty,
                                                    fallback = false)
    val hits = results.results
    results.totalCount should be(9)
    hits.head.id should be(8)
    hits(1).id should be(1)
    hits(2).id should be(3)
    hits(3).id should be(9)
    hits(4).id should be(5)
    hits(5).id should be(11)
    hits(6).id should be(6)
    hits(7).id should be(2)
    hits.last.id should be(7)
  }

  test("That all returns all documents ordered by title descending") {
    val Success(results) = articleSearchService.all(List(),
                                                    Language.DefaultLanguage,
                                                    None,
                                                    1,
                                                    10,
                                                    Sort.ByTitleDesc,
                                                    Seq.empty,
                                                    fallback = false)
    val hits = results.results
    results.totalCount should be(9)
    hits.head.id should be(7)
    hits(1).id should be(2)
    hits(2).id should be(6)
    hits(3).id should be(11)
    hits(4).id should be(5)
    hits(5).id should be(9)
    hits(6).id should be(3)
    hits(7).id should be(1)
    hits.last.id should be(8)
  }

  test("That all returns all documents ordered by lastUpdated descending") {
    val results = articleSearchService.all(List(),
                                           Language.DefaultLanguage,
                                           None,
                                           1,
                                           10,
                                           Sort.ByLastUpdatedDesc,
                                           Seq.empty,
                                           fallback = false)
    val hits = results.get.results
    results.get.totalCount should be(9)
    hits.head.id should be(3)
    hits.last.id should be(5)
  }

  test("That all returns all documents ordered by lastUpdated ascending") {
    val Success(results) = articleSearchService.all(List(),
                                                    Language.DefaultLanguage,
                                                    None,
                                                    1,
                                                    10,
                                                    Sort.ByLastUpdatedAsc,
                                                    Seq.empty,
                                                    fallback = false)
    val hits = results.results
    results.totalCount should be(9)
    hits.head.id should be(5)
    hits(1).id should be(6)
    hits(2).id should be(7)
    hits(3).id should be(8)
    hits(4).id should be(9)
    hits(5).id should be(11)
    hits(6).id should be(1)
    hits(7).id should be(2)
    hits.last.id should be(3)
  }

  test("That all filtering on license only returns documents with given license") {
    val Success(results) = articleSearchService.all(List(),
                                                    Language.DefaultLanguage,
                                                    Some(PublicDomain.toString),
                                                    1,
                                                    10,
                                                    Sort.ByTitleAsc,
                                                    Seq.empty,
                                                    fallback = false)
    val hits = results.results
    results.totalCount should be(8)
    hits.head.id should be(8)
    hits(1).id should be(3)
    hits(2).id should be(9)
    hits(3).id should be(5)
    hits(4).id should be(11)
    hits(5).id should be(6)
    hits(6).id should be(2)
    hits.last.id should be(7)
  }

  test("That all filtered by id only returns documents with the given ids") {
    val Success(results) = articleSearchService.all(List(1, 3),
                                                    Language.DefaultLanguage,
                                                    None,
                                                    1,
                                                    10,
                                                    Sort.ByIdAsc,
                                                    Seq.empty,
                                                    fallback = false)
    val hits = results.results
    results.totalCount should be(2)
    hits.head.id should be(1)
    hits.last.id should be(3)
  }

  test("That paging returns only hits on current page and not more than page-size") {
    val Success(page1) = articleSearchService.all(List(),
                                                  Language.DefaultLanguage,
                                                  None,
                                                  1,
                                                  2,
                                                  Sort.ByTitleAsc,
                                                  Seq.empty,
                                                  fallback = false)
    val Success(page2) = articleSearchService.all(List(),
                                                  Language.DefaultLanguage,
                                                  None,
                                                  2,
                                                  2,
                                                  Sort.ByTitleAsc,
                                                  Seq.empty,
                                                  fallback = false)
    val hits1 = page1.results
    val hits2 = page2.results
    page1.totalCount should be(9)
    page1.page.get should be(1)
    hits1.size should be(2)
    hits1.head.id should be(8)
    hits1.last.id should be(1)
    page2.totalCount should be(9)
    page2.page.get should be(2)
    hits2.size should be(2)
    hits2.head.id should be(3)
    hits2.last.id should be(9)
  }

  test("matchingQuery should filter results based on an article type filter") {
    val results = articleSearchService.matchingQuery("bil",
                                                     List(),
                                                     "nb",
                                                     None,
                                                     1,
                                                     10,
                                                     Sort.ByRelevanceDesc,
                                                     Seq(ArticleType.TopicArticle.toString),
                                                     fallback = false)
    results.get.totalCount should be(0)

    val results2 = articleSearchService.matchingQuery("bil",
                                                      List(),
                                                      "nb",
                                                      None,
                                                      1,
                                                      10,
                                                      Sort.ByRelevanceDesc,
                                                      Seq(ArticleType.Standard.toString),
                                                      fallback = false)
    results2.get.totalCount should be(3)
  }

  test("That search matches title and html-content ordered by relevance descending") {
    val Success(results) = articleSearchService.matchingQuery("bil",
                                                              List(),
                                                              "nb",
                                                              None,
                                                              1,
                                                              10,
                                                              Sort.ByRelevanceDesc,
                                                              Seq.empty,
                                                              fallback = false)
    val hits = results.results
    results.totalCount should be(3)
    hits.head.id should be(5)
    hits(1).id should be(1)
    hits.last.id should be(3)
  }

  test("That search combined with filter by id only returns documents matching the query with one of the given ids") {
    val Success(results) = articleSearchService.matchingQuery("bil",
                                                              List(3),
                                                              "nb",
                                                              None,
                                                              1,
                                                              10,
                                                              Sort.ByRelevanceDesc,
                                                              Seq.empty,
                                                              fallback = false)
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(3)
    hits.last.id should be(3)
  }

  test("That search matches title") {
    val Success(results) = articleSearchService.matchingQuery("Pingvinen",
                                                              List(),
                                                              "nb",
                                                              None,
                                                              1,
                                                              10,
                                                              Sort.ByTitleAsc,
                                                              Seq.empty,
                                                              fallback = false)
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(2)
  }

  test("That search matches tags") {
    val Success(results) =
      articleSearchService.matchingQuery("and", List(), "nb", None, 1, 10, Sort.ByTitleAsc, Seq.empty, fallback = false)
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(3)
  }

  test("That search does not return superman since it has license copyrighted and license is not specified") {
    val Success(results) = articleSearchService.matchingQuery("supermann",
                                                              List(),
                                                              "nb",
                                                              None,
                                                              1,
                                                              10,
                                                              Sort.ByTitleAsc,
                                                              Seq.empty,
                                                              fallback = false)
    results.totalCount should be(0)
  }

  test("That search returns superman since license is specified as copyrighted") {
    val Success(results) = articleSearchService.matchingQuery("supermann",
                                                              List(),
                                                              "nb",
                                                              Some(Copyrighted.toString),
                                                              1,
                                                              10,
                                                              Sort.ByTitleAsc,
                                                              Seq.empty,
                                                              fallback = false)
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(4)
  }

  test("Searching with logical AND only returns results with all terms") {
    val Success(search1) = articleSearchService.matchingQuery("bilde + bil",
                                                              List(),
                                                              "nb",
                                                              None,
                                                              1,
                                                              10,
                                                              Sort.ByTitleAsc,
                                                              Seq.empty,
                                                              fallback = false)
    val hits1 = search1.results
    hits1.map(_.id) should equal(Seq(1, 3, 5))

    val Success(search2) = articleSearchService.matchingQuery("batmen + bil",
                                                              List(),
                                                              "nb",
                                                              None,
                                                              1,
                                                              10,
                                                              Sort.ByTitleAsc,
                                                              Seq.empty,
                                                              fallback = false)
    val hits2 = search2.results
    hits2.map(_.id) should equal(Seq(1))

    val Success(search3) = articleSearchService.matchingQuery("bil + bilde + -flaggermusmann",
                                                              List(),
                                                              "nb",
                                                              None,
                                                              1,
                                                              10,
                                                              Sort.ByTitleAsc,
                                                              Seq.empty,
                                                              fallback = false)
    val hits3 = search3.results
    hits3.map(_.id) should equal(Seq(3, 5))

    val Success(search4) = articleSearchService.matchingQuery("bil + -hulken",
                                                              List(),
                                                              "nb",
                                                              None,
                                                              1,
                                                              10,
                                                              Sort.ByTitleAsc,
                                                              Seq.empty,
                                                              fallback = false)
    val hits4 = search4.results
    hits4.map(_.id) should equal(Seq(1, 3))
  }

  test("search in content should be ranked lower than introduction and title") {
    val Success(search) = articleSearchService.matchingQuery("mareritt+ragnarok",
                                                             List(),
                                                             "nb",
                                                             None,
                                                             1,
                                                             10,
                                                             Sort.ByRelevanceDesc,
                                                             Seq.empty,
                                                             fallback = false)
    val hits = search.results
    hits.map(_.id) should equal(Seq(9, 8))
  }

  test("Search for all languages should return all articles in different languages") {
    val Success(search) = articleSearchService.all(List(),
                                                   Language.AllLanguages,
                                                   None,
                                                   1,
                                                   100,
                                                   Sort.ByTitleAsc,
                                                   Seq.empty,
                                                   fallback = false)

    search.totalCount should equal(10)
  }

  test("Search for all languages should return all articles in correct language") {
    val Success(search) =
      articleSearchService.all(List(), Language.AllLanguages, None, 1, 100, Sort.ByIdAsc, Seq.empty, fallback = false)
    val hits = search.results

    search.totalCount should equal(10)
    hits(0).id should equal(1)
    hits(1).id should equal(2)
    hits(2).id should equal(3)
    hits(3).id should equal(5)
    hits(4).id should equal(6)
    hits(5).id should equal(7)
    hits(6).id should equal(8)
    hits(7).id should equal(9)
    hits(8).id should equal(10)
    hits(9).id should equal(11)
    hits(8).title.language should equal("en")
    hits(9).title.language should equal("nb")
  }

  test("Search for all languages should return all languages if copyrighted") {
    val Success(search) = articleSearchService.all(List(),
                                                   Language.AllLanguages,
                                                   Some(Copyrighted.toString),
                                                   1,
                                                   100,
                                                   Sort.ByTitleAsc,
                                                   Seq.empty,
                                                   fallback = false)
    val hits = search.results

    search.totalCount should equal(1)
    hits.head.id should equal(4)
  }

  test("Searching with query for all languages should return language that matched") {
    val Success(searchEn) = articleSearchService.matchingQuery("Cats",
                                                               List(),
                                                               "all",
                                                               None,
                                                               1,
                                                               10,
                                                               Sort.ByRelevanceDesc,
                                                               Seq.empty,
                                                               fallback = false)
    val Success(searchNb) = articleSearchService.matchingQuery("Katter",
                                                               List(),
                                                               "all",
                                                               None,
                                                               1,
                                                               10,
                                                               Sort.ByRelevanceDesc,
                                                               Seq.empty,
                                                               fallback = false)

    searchEn.totalCount should equal(1)
    searchEn.results.head.id should equal(11)
    searchEn.results.head.title.title should equal("Cats")
    searchEn.results.head.title.language should equal("en")

    searchNb.totalCount should equal(1)
    searchNb.results.head.id should equal(11)
    searchNb.results.head.title.title should equal("Katter")
    searchNb.results.head.title.language should equal("nb")
  }

  test("metadescription is searchable") {
    val Success(search) = articleSearchService.matchingQuery("hurr dirr",
                                                             List(),
                                                             "all",
                                                             None,
                                                             1,
                                                             10,
                                                             Sort.ByRelevanceDesc,
                                                             Seq.empty,
                                                             fallback = false)

    search.totalCount should equal(1)
    search.results.head.id should equal(11)
    search.results.head.title.title should equal("Cats")
    search.results.head.title.language should equal("en")
  }

  test("That searching with fallback parameter returns article in language priority even if doesnt match on language") {
    val Success(search) =
      articleSearchService.all(List(9, 10, 11), "en", None, 1, 10, Sort.ByIdAsc, Seq.empty, fallback = true)

    search.totalCount should equal(3)
    search.results.head.id should equal(9)
    search.results.head.title.language should equal("nb")
    search.results(1).id should equal(10)
    search.results(1).title.language should equal("en")
    search.results(2).id should equal(11)
    search.results(2).title.language should equal("en")
  }

  test("That metaImage altText is included in the search") {
    val Success(search) = articleSearchService.all(List(1), "nb", None, 1, 10, Sort.ByIdAsc, Seq.empty, fallback = true)
    search.totalCount should be(1)
    search.results.head.metaImage should be(
      Some(
        api.ArticleMetaImage("http://api-gateway.ndla-local/image-api/raw/id/5555", "Alt text is here friend", "nb")
      ))
  }

  test("That scrolling works as expected") {
    val pageSize = 2
    val expectedIds = List(1, 2, 3, 5, 6, 7, 8, 9, 10, 11).sliding(pageSize, pageSize).toList

    val Success(initialSearch) =
      articleSearchService.all(List.empty, "all", None, 1, pageSize, Sort.ByIdAsc, Seq.empty, true)

    val Success(scroll1) = articleSearchService.scroll(initialSearch.scrollId.get, "all", true)
    val Success(scroll2) = articleSearchService.scroll(scroll1.scrollId.get, "all", true)
    val Success(scroll3) = articleSearchService.scroll(scroll2.scrollId.get, "all", true)
    val Success(scroll4) = articleSearchService.scroll(scroll3.scrollId.get, "all", true)
    val Success(scroll5) = articleSearchService.scroll(scroll4.scrollId.get, "all", true)

    initialSearch.results.map(_.id) should be(expectedIds.head)
    scroll1.results.map(_.id) should be(expectedIds(1))
    scroll2.results.map(_.id) should be(expectedIds(2))
    scroll3.results.map(_.id) should be(expectedIds(3))
    scroll4.results.map(_.id) should be(expectedIds(4))
    scroll5.results.map(_.id) should be(List.empty)
  }

  test("That highlighting works when scrolling") {
    val Success(initialSearch) =
      articleSearchService.matchingQuery("about", List.empty, "all", None, 1, 1, Sort.ByIdAsc, Seq.empty, true)
    val Success(scroll) = articleSearchService.scroll(initialSearch.scrollId.get, "all", true)

    initialSearch.results.size should be(1)
    initialSearch.results.head.id should be(10)

    scroll.results.size should be(1)
    scroll.results.head.id should be(11)
    scroll.results.head.title.language should be("en")
    scroll.results.head.title.title should be("Cats")
  }

  def blockUntil(predicate: () => Boolean): Unit = {
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
