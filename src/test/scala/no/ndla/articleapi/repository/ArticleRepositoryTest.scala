/*
 * Part of NDLA article_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.repository

import java.net.Socket

import no.ndla.articleapi.model.api.NotFoundException
import no.ndla.articleapi.model.domain
import no.ndla.articleapi.model.domain.ArticleIds
import no.ndla.articleapi._
import no.ndla.articleapi.integration.DataSource
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool}

import scala.util.{Failure, Success, Try}

class ArticleRepositoryTest extends IntegrationSuite with TestEnvironment {
  var repository: ArticleRepository = new ArticleRepository

  lazy val sampleArticle = TestData.sampleArticleWithByNcSa

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
    val ds = DataSource.getHikariDataSource
    ConnectionPool.singleton(new DataSourceConnectionPool(ds))
    if (serverIsListening) {
      DBMigrator.migrate(ds)
    }
  }

  override def beforeEach(): Unit =
    if (databaseIsAvailable) repository.getAllIds().foreach(articleId => repository.delete(articleId.articleId))

  override def afterEach(): Unit =
    if (databaseIsAvailable) repository.getAllIds().foreach(articleId => repository.delete(articleId.articleId))

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

    repository.getArticleIdsFromExternalId("6011").get.externalId should be(externalIds)
    repository.delete(inserted.get.id.get)
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
    val idWithExternals = repository.updateArticleFromDraftApi(sampleArticle.copy(id = Some(1)), externalIds)
    val idWithoutExternals = repository.updateArticleFromDraftApi(sampleArticle.copy(id = Some(2)), List.empty)

    val result1 = repository.getExternalIdsFromId(idWithExternals.get.id.get)
    result1 should be(externalIds)
    val result2 = repository.getExternalIdsFromId(idWithoutExternals.get.id.get)
    result2 should be(List.empty)

    repository.delete(idWithExternals.get.id.get)
    repository.delete(idWithoutExternals.get.id.get)
  }

  test("updating with a valid article with a that is not in database will be recreated") {
    assume(databaseIsAvailable, "Database is unavailable")
    val article = TestData.sampleDomainArticle.copy(id = Some(110))

    val x = repository.updateArticleFromDraftApi(article, List.empty)
    x.isSuccess should be(true)

    repository.delete(x.get.id.get)
  }

  test("deleting article should ignore missing articles") {
    assume(databaseIsAvailable, "Database is unavailable")
    val article = TestData.sampleDomainArticle.copy(id = Some(Integer.MAX_VALUE))

    val deletedId = repository.delete(article.id.get)
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

}
