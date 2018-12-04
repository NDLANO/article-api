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
    val externalIds = (100 to 150).map(_.toString)
    val ids = externalIds.map(exId => repository.allocateArticleIdWithExternalIds(List(exId), Set("52")))
    val expected = ids.zip(externalIds).map { case (id, exId) => ArticleIds(id, List(exId)) }.toList

    repository.getAllIds should equal(expected)
  }

  test("getIdFromExternalId works with all ids") {
    assume(databaseIsAvailable, "Database is unavailable")
    val inserted1 = repository.allocateArticleIdWithExternalIds(List("6000", "10"), Set("52"))
    val inserted2 = repository.allocateArticleIdWithExternalIds(List("6001", "11"), Set("52"))

    repository.getIdFromExternalId("6000").get should be(inserted1)
    repository.getIdFromExternalId("6001").get should be(inserted2)
    repository.getIdFromExternalId("10").get should be(inserted1)
    repository.getIdFromExternalId("11").get should be(inserted2)
  }

  test("getArticleIdsFromExternalId should return ArticleIds object with externalIds") {
    assume(databaseIsAvailable, "Database is unavailable")
    val externalIds = List("1", "6010", "6011", "5084", "763", "8881", "1919")
    val inserted = repository.allocateArticleIdWithExternalIds(externalIds, Set.empty)

    repository.getArticleIdsFromExternalId("6011").get.externalId should be(externalIds)
    repository.delete(inserted)
  }

  test("updateArticleFromDraftApi should update all columns with data from draft-api") {
    assume(databaseIsAvailable, "Database is unavailable")

    val externalIds = List("123", "456")
    val sampleArticle: domain.Article =
      TestData.sampleDomainArticle.copy(id = Some(repository.allocateArticleId()), revision = Some(42))

    sampleArticle.created.setTime(0)
    sampleArticle.created.setTime(0)
    val Success((res: domain.Article)) = repository.updateArticleFromDraftApi(sampleArticle, externalIds)

    res.id.isDefined should be(true)
    repository.withId(res.id.get).get should be(sampleArticle)
  }

  test("Fetching external ids works as expected") {
    assume(databaseIsAvailable, "Database is unavailable")

    val externalIds = List("1", "2", "3")
    val idWithExternals = repository.allocateArticleIdWithExternalIds(externalIds, Set.empty)
    val idWithoutExternals = repository.allocateArticleId()

    val result1 = repository.getExternalIdsFromId(idWithExternals)
    result1 should be(externalIds)
    val result2 = repository.getExternalIdsFromId(idWithoutExternals)
    result2 should be(List.empty)

    repository.delete(idWithExternals)
    repository.delete(idWithoutExternals)
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

}
