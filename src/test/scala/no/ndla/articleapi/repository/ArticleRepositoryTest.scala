/*
 * Part of NDLA article_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.repository

import java.net.Socket

import no.ndla.articleapi._
import no.ndla.articleapi.model.domain
import no.ndla.articleapi.model.domain.{Article, ArticleIds, ArticleTag}
import no.ndla.scalatestsuite.IntegrationSuite
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool}

import scala.util.{Success, Try}

class ArticleRepositoryTest
    extends IntegrationSuite(EnablePostgresContainer = true)
    with UnitSuite
    with TestEnvironment {
  override val dataSource = testDataSource.get
  var repository: ArticleRepository = _

  lazy val sampleArticle: Article = TestData.sampleArticleWithByNcSa

  def serverIsListening: Boolean = {
    Try(new Socket(ArticleApiProperties.MetaServer, ArticleApiProperties.MetaPort)) match {
      case Success(c) =>
        c.close()
        true
      case _ => false
    }
  }

  def databaseIsAvailable: Boolean = Try(repository.articleCount).isSuccess

  override def beforeAll(): Unit = {
    super.beforeAll()
    Try {
      ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))
      if (serverIsListening) {
        DBMigrator.migrate(dataSource)
      }
    }
  }

  override def beforeEach(): Unit = {
    repository = new ArticleRepository
    if (databaseIsAvailable)
      repository.getAllIds().foreach(articleId => repository.deleteMaxRevision(articleId.articleId))
  }

  override def afterEach(): Unit =
    if (databaseIsAvailable)
      repository.getAllIds().foreach(articleId => repository.deleteMaxRevision(articleId.articleId))

  test("getAllIds returns a list with all ids in the database") {
    assume(databaseIsAvailable, "Database is unavailable")
    val externalIdsAndRegularIds = (100 to 150).map(_.toString).zipWithIndex
    externalIdsAndRegularIds.foreach {
      case (exId, id) => repository.updateArticleFromDraftApi(sampleArticle.copy(id = Some(id)), List(exId))
    }
    val expected = externalIdsAndRegularIds.map { case (exId, id) => ArticleIds(id, List(exId)) }.toList
    repository.getAllIds should equal(expected)
  }

  test("getIdFromExternalId works with all ids") {
    assume(databaseIsAvailable, "Database is unavailable")
    val inserted1 = repository.updateArticleFromDraftApi(sampleArticle.copy(id = Some(1)), List("6000", "10"))
    val inserted2 = repository.updateArticleFromDraftApi(sampleArticle.copy(id = Some(2)), List("6001", "11"))

    repository.getIdFromExternalId("6000").get should be(inserted1.get.id.get)
    repository.getIdFromExternalId("6001").get should be(inserted2.get.id.get)
    repository.getIdFromExternalId("10").get should be(inserted1.get.id.get)
    repository.getIdFromExternalId("11").get should be(inserted2.get.id.get)
  }

  test("getArticleIdsFromExternalId should return ArticleIds object with externalIds") {
    assume(databaseIsAvailable, "Database is unavailable")
    val externalIds = List("1", "6010", "6011", "5084", "763", "8881", "1919")
    val inserted = repository.updateArticleFromDraftApi(sampleArticle, externalIds)
    val inserted2 = repository.updateArticleFromDraftApi(sampleArticle.copy(revision = Some(2)), externalIds)

    repository.getArticleIdsFromExternalId("6011").get.externalId should be(externalIds)
    repository.deleteMaxRevision(inserted.get.id.get)
    repository.deleteMaxRevision(inserted2.get.id.get)
  }

  test("updateArticleFromDraftApi should update all columns with data from draft-api") {
    assume(databaseIsAvailable, "Database is unavailable")

    val externalIds = List("123", "456")
    val sampleArticle: domain.Article =
      TestData.sampleDomainArticle.copy(id = Some(5), revision = Some(42))

    sampleArticle.created.setTime(0)
    sampleArticle.created.setTime(0)
    val Success(res: domain.Article) = repository.updateArticleFromDraftApi(sampleArticle, externalIds)

    res.id.isDefined should be(true)
    repository.withId(res.id.get).get should be(sampleArticle)
  }

  test("Fetching external ids works as expected") {
    assume(databaseIsAvailable, "Database is unavailable")

    val externalIds = List("1", "2", "3")
    val idWithExternals = repository.updateArticleFromDraftApi(sampleArticle.copy(id = Some(1), revision = Some(1)), externalIds)
    val idWithExternals2 = repository.updateArticleFromDraftApi(sampleArticle.copy(id = Some(1), revision = Some(2)), externalIds)
    val idWithoutExternals = repository.updateArticleFromDraftApi(sampleArticle.copy(id = Some(2)), List.empty)

    val result1 = repository.getExternalIdsFromId(idWithExternals.get.id.get)
    result1 should be(externalIds)
    val result2 = repository.getExternalIdsFromId(idWithoutExternals.get.id.get)
    result2 should be(List.empty)

    repository.deleteMaxRevision(idWithExternals.get.id.get)
    repository.deleteMaxRevision(idWithoutExternals.get.id.get)
  }

  test("updating with a valid article with a that is not in database will be recreated") {
    assume(databaseIsAvailable, "Database is unavailable")
    val article = TestData.sampleDomainArticle.copy(id = Some(110))

    val x = repository.updateArticleFromDraftApi(article, List.empty)
    x.isSuccess should be(true)

    repository.deleteMaxRevision(x.get.id.get)
  }

  test("deleting article should ignore missing articles") {
    assume(databaseIsAvailable, "Database is unavailable")
    val article = TestData.sampleDomainArticle.copy(id = Some(Integer.MAX_VALUE))

    val deletedId = repository.deleteMaxRevision(article.id.get)
    deletedId.get should be(Integer.MAX_VALUE)
  }

  test("That getArticlesByPage returns all latest articles") {
    assume(databaseIsAvailable, "Database is unavailable")

    val art1 = repository.updateArticleFromDraftApi(sampleArticle.copy(id = Some(1)), List.empty).get
    val art2 = repository.updateArticleFromDraftApi(sampleArticle.copy(id = Some(2)), List.empty).get
    val art3 = repository.updateArticleFromDraftApi(sampleArticle.copy(id = Some(3)), List.empty).get
    val art4 = repository.updateArticleFromDraftApi(sampleArticle.copy(id = Some(4)), List.empty).get
    val art5 = repository.updateArticleFromDraftApi(sampleArticle.copy(id = Some(5)), List.empty).get
    val art6 = repository.updateArticleFromDraftApi(sampleArticle.copy(id = Some(6)), List.empty).get

    val pageSize = 4
    repository.getArticlesByPage(pageSize, pageSize * 0) should be(Seq(art1, art2, art3, art4))
    repository.getArticlesByPage(pageSize, pageSize * 1) should be(Seq(art5, art6))
  }

  test("That stored articles are retrieved exactly as they were stored") {
    assume(databaseIsAvailable, "Database is unavailable")

    val art1 = repository.updateArticleFromDraftApi(TestData.sampleArticleWithByNcSa.copy(id = Some(1)), List.empty).get
    val art2 =
      repository.updateArticleFromDraftApi(TestData.sampleArticleWithPublicDomain.copy(id = Some(2)), List.empty).get
    val art3 =
      repository.updateArticleFromDraftApi(TestData.sampleArticleWithCopyrighted.copy(id = Some(3)), List.empty).get

    repository.withId(1) should be(Some(art1))
    repository.withId(2) should be(Some(art2))
    repository.withId(3) should be(Some(art3))
  }

  test("getTags returns non-duplicate tags and correct number of them") {
    assume(databaseIsAvailable, "Database is unavailable")
    val sampleArticle1 = TestData.sampleDomainArticle2
      .copy(id = Some(1L),
            revision = Some(0),
            tags = Seq(ArticleTag(Seq("abc", "bcd", "ddd"), "nb"), ArticleTag(Seq("abc", "bcd"), "nn")))
    val sampleArticle2 = TestData.sampleDomainArticle2
      .copy(id = Some(2L),
            revision = Some(0),
            tags = Seq(ArticleTag(Seq("bcd", "cde"), "nb"), ArticleTag(Seq("bcd", "cde"), "nn")))
    val sampleArticle3 =
      TestData.sampleDomainArticle2
        .copy(id = Some(3L),
              revision = Some(0),
              tags = Seq(ArticleTag(Seq("def"), "nb"), ArticleTag(Seq("d", "def", "asd"), "nn")))
    val sampleArticle4 = TestData.sampleDomainArticle2.copy(id = Some(4L), revision = Some(0), tags = Seq.empty)

    repository.updateArticleFromDraftApi(sampleArticle1, List.empty)
    repository.updateArticleFromDraftApi(sampleArticle2, List.empty)
    repository.updateArticleFromDraftApi(sampleArticle3, List.empty)
    repository.updateArticleFromDraftApi(sampleArticle4, List.empty)

    val (tags1, tagsCount1) = repository.getTags("", 5, 0, "nb")
    tags1 should equal(Seq("abc", "bcd", "cde", "ddd", "def"))
    tags1.length should be(5)
    tagsCount1 should be(5)

    val (tags2, tagsCount2) = repository.getTags("", 2, 0, "nb")
    tags2 should equal(Seq("abc", "bcd"))
    tags2.length should be(2)
    tagsCount2 should be(5)

    val (tags3, tagsCount3) = repository.getTags("", 2, 3, "nn")
    tags3 should equal(Seq("cde", "d"))
    tags3.length should be(2)
    tagsCount3 should be(6)

    val (tags4, tagsCount4) = repository.getTags("", 1, 3, "nn")
    tags4 should equal(Seq("cde"))
    tags4.length should be(1)
    tagsCount4 should be(6)

    val (tags5, tagsCount5) = repository.getTags("", 10, 0, "all")
    tags5 should equal(Seq("abc", "asd", "bcd", "cde", "d", "ddd", "def"))
    tags5.length should be(7)
    tagsCount5 should be(7)

    val (tags6, tagsCount6) = repository.getTags("d", 5, 0, "")
    tags6 should equal(Seq("d", "ddd", "def"))
    tags6.length should be(3)
    tagsCount6 should be(3)

    val (tags7, tagsCount7) = repository.getTags("%b", 5, 0, "")
    tags7 should equal(Seq("bcd"))
    tags7.length should be(1)
    tagsCount7 should be(1)

    val (tags8, tagsCount8) = repository.getTags("a", 10, 0, "")
    tags8 should equal(Seq("abc", "asd"))
    tags8.length should be(2)
    tagsCount8 should be(2)

    val (tags9, tagsCount9) = repository.getTags("A", 10, 0, "")
    tags9 should equal(Seq("abc", "asd"))
    tags9.length should be(2)
    tagsCount9 should be(2)
  }

  test("withId parse relatedContent correctly") {
    repository.updateArticleFromDraftApi(sampleArticle.copy(id = Some(1), relatedContent = Seq(Right(2))),
                                         List("6000", "10"))

    val Right(relatedId) = repository.withId(1).get.relatedContent.head
    relatedId.toLong should be(2L)

  }

}
