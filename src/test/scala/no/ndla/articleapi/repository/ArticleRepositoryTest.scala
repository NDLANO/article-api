package no.ndla.articleapi.repository

import no.ndla.articleapi.model.domain.{ArticleIds, ArticleTitle}
import no.ndla.articleapi.{DBMigrator, IntegrationSuite, TestData, TestEnvironment}
import no.ndla.tag.IntegrationTest
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool}

@IntegrationTest
class ArticleRepositoryTest extends IntegrationSuite with TestEnvironment {
  var repository: ArticleRepository = _

  val sampleArticle = TestData.sampleArticleWithByNcSa

  override def beforeEach() = {
    repository = new ArticleRepository()
  }

  override def beforeAll() = {
    ConnectionPool.singleton(new DataSourceConnectionPool(getDataSource))
    DBMigrator.migrate(ConnectionPool.dataSource())
  }

  test("getAllIds returns a list with all ids in the database") {
    val externalIds = (100 to 150).map(_.toString)
    val ids = externalIds.map(exId => repository.allocateArticleIdWithExternalIds(List(exId), Set("52")))
    val expected = ids.zip(externalIds).map { case (id, exId) => ArticleIds(id, List(exId)) }.toList

    repository.getAllIds should equal(expected)

    ids.foreach(repository.delete)
  }

  test("getIdFromExternalId works with all ids") {
    val inserted1 = repository.allocateArticleIdWithExternalIds(List("6000","10"), Set("52"))
    val inserted2 = repository.allocateArticleIdWithExternalIds(List("6001","11"), Set("52"))

    repository.getIdFromExternalId("6000").get should be(inserted1)
    repository.getIdFromExternalId("6001").get should be(inserted2)
    repository.getIdFromExternalId("10").get should be(inserted1)
    repository.getIdFromExternalId("11").get should be(inserted2)

    repository.delete(inserted1)
    repository.delete(inserted2)
  }

  test("getArticleIdsFromExternalId should return ArticleIds object with externalIds") {
    val externalIds = List("1", "6010", "6011", "5084", "763", "8881", "1919")
    val inserted = repository.allocateArticleIdWithExternalIds(externalIds, Set.empty)

    repository.getArticleIdsFromExternalId("6011").get.externalId should be(externalIds)
    repository.delete(inserted)
  }

}
