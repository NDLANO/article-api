package no.ndla.articleapi.service.search

import com.sksamuel.elastic4s.http.ElasticDsl._
import no.ndla.articleapi._
import no.ndla.articleapi.integration.Elastic4sClientFactory
import no.ndla.tag.IntegrationTest

import scala.util.Success

@IntegrationTest
class IndexServiceTest extends UnitSuite with TestEnvironment {

  val esPort = 9200

  override val e4sClient = Elastic4sClientFactory.getClient(searchServer = s"http://localhost:$esPort")
  val testIndexPrefix = "article-index-service-test"
  override val articleIndexService = new ArticleIndexService {
    override val searchIndex = s"${testIndexPrefix}_article"
  }

  private def deleteIndexesThatStartWith(startsWith: String): Unit = {
    val Success(result) = e4sClient.execute(getAliases())
    val toDelete = result.result.mappings.filter(_._1.name.startsWith(startsWith)).map(_._1.name)

    if(toDelete.nonEmpty) {
      e4sClient.execute(deleteIndex(toDelete))
    }
  }

  test("That cleanupIndexes does not delete others indexes") {
    val image1Name = s"${testIndexPrefix}_image_1"
    val article1Name = s"${testIndexPrefix}_article_1"
    val article2Name = s"${testIndexPrefix}_article_2"
    val concept1Name = s"${testIndexPrefix}_concept_1"

    articleIndexService.createIndexWithName(image1Name)
    articleIndexService.createIndexWithName(article1Name)
    articleIndexService.createIndexWithName(article2Name)
    articleIndexService.createIndexWithName(concept1Name)
    articleIndexService.updateAliasTarget(None, article1Name)

    articleIndexService.cleanupIndexes(s"${testIndexPrefix}_article")

    val Success(response) = e4sClient.execute(getAliases())
    val result = response.result.mappings
    val indexNames = result.map(_._1.name)

    indexNames should contain(image1Name)
    indexNames should contain(article1Name)
    indexNames should contain(concept1Name)
    indexNames should not contain article2Name

    articleIndexService.cleanupIndexes(testIndexPrefix)
  }

  override def afterAll =  {
    deleteIndexesThatStartWith(testIndexPrefix)
  }
}
