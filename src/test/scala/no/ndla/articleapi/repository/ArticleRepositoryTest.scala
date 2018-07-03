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

  test("updating several times updates revision number") {
    val first = repository.newArticle(sampleArticle)
    first.id.isDefined should be (true)

    val second = repository.updateArticle(first.copy(title = Seq(ArticleTitle("first change", "en"))))
    second.isSuccess should be (true)

    val third = repository.updateArticle(second.get.copy(title = Seq(ArticleTitle("second change", "en"))))
    third.isSuccess should be (true)

    first.revision should equal (Some(1))
    second.get.revision should equal(Some(2))
    third.get.revision should equal(Some(3))

    repository.delete(first.id.get)
  }

  test("Updating with an outdated revision number returns a Failure") {
    val first = repository.newArticle(sampleArticle)
    first.id.isDefined should be (true)

    val oldRevision = repository.updateArticle(first.copy(revision=Some(0), title = Seq(ArticleTitle("first change", "en"))))
    oldRevision.isFailure should be (true)

    val tooNewRevision = repository.updateArticle(first.copy(revision=Some(99), title = Seq(ArticleTitle("first change", "en"))))
    tooNewRevision.isFailure should be (true)

    repository.delete(first.id.get)
  }

  test("updateWithExternalId does not update revision number") {
    val externalId = "123"
    val articleId = repository.insertWithExternalIds(sampleArticle, Seq(externalId), Seq("52")).id.get

    val firstUpdate = repository.updateWithExternalId(sampleArticle, externalId)
    val secondUpdate = repository.updateWithExternalId(sampleArticle.copy(title = Seq(ArticleTitle("new title", "en"))), externalId)

    firstUpdate.isSuccess should be (true)
    secondUpdate.isSuccess should be (true)

    val updatedArticle = repository.withId(articleId)
    updatedArticle.isDefined should be (true)
    updatedArticle.get.revision should be (Some(1))

    repository.delete(articleId)
  }

  test("updateWithExternalId returns a Failure if article has been updated on new platform") {
    val externalId = "123"
    val article = repository.insertWithExternalIds(sampleArticle, Seq(externalId), Seq("52"))

    repository.updateArticle(sampleArticle.copy(id=article.id))
    val result = repository.updateWithExternalId(sampleArticle.copy(id=article.id), externalId)
    result.isFailure should be (true)

    repository.delete(article.id.get)
  }

  test("getAllIds returns a list with all ids in the database") {
    val externalIds = (100 to 150).map(_.toString)
    val ids = externalIds.map(exId => repository.insertWithExternalIds(sampleArticle, Seq(exId), Seq("52")).id.get)
    val expected = ids.zip(externalIds).map { case (id, exId) => ArticleIds(id, List(exId)) }.toList

    repository.getAllIds should equal(expected)

    ids.foreach(repository.delete)
  }

  test("getIdFromExternalId works with all ids") {
    val inserted1 = repository.insertWithExternalIds(sampleArticle, Seq("6000","10"), Seq("52")).id.get
    val inserted2 = repository.insertWithExternalIds(sampleArticle, Seq("6001","11"), Seq("52")).id.get

    repository.getIdFromExternalId("6000").get should be(inserted1)
    repository.getIdFromExternalId("6001").get should be(inserted2)
    repository.getIdFromExternalId("10").get should be(inserted1)
    repository.getIdFromExternalId("11").get should be(inserted2)

    repository.delete(inserted1)
    repository.delete(inserted2)
  }

}
