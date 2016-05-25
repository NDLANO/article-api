package no.ndla.contentapi

import javax.sql.DataSource

import com.sksamuel.elastic4s.ElasticClient
import no.ndla.contentapi.integration.{DataSourceComponent, ElasticClientComponent}
import no.ndla.contentapi.repository.ContentRepositoryComponent
import no.ndla.contentapi.service.{ElasticContentIndexComponent, ElasticContentSearchComponent}
import org.scalatest.mock.MockitoSugar


trait TestEnvironment
  extends ElasticClientComponent
  with ElasticContentSearchComponent
  with ElasticContentIndexComponent
  with DataSourceComponent
  with ContentRepositoryComponent
  with MockitoSugar
{
  val elasticClient = mock[ElasticClient]
  val elasticContentSearch = mock[ElasticContentSearch]
  val elasticContentIndex = mock[ElasticContentIndex]

  val dataSource = mock[DataSource]
  val contentRepository = new ContentRepository
}
