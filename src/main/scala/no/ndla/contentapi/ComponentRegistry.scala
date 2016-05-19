package no.ndla.contentapi

import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import no.ndla.contentapi.integration.{DataSourceComponent, ElasticClientComponent}
import no.ndla.contentapi.repository.ContentRepositoryComponent
import no.ndla.contentapi.service.{ElasticContentIndexComponent, ElasticContentSearchComponent}
import org.elasticsearch.common.settings.ImmutableSettings
import org.postgresql.ds.PGPoolingDataSource


object ComponentRegistry
  extends DataSourceComponent
  with ContentRepositoryComponent
  with ElasticClientComponent
  with ElasticContentSearchComponent
  with ElasticContentIndexComponent {

  val dataSource = new PGPoolingDataSource()
  dataSource.setUser(ContentApiProperties.get("META_USER_NAME"))
  dataSource.setPassword(ContentApiProperties.get("META_PASSWORD"))
  dataSource.setDatabaseName(ContentApiProperties.get("META_RESOURCE"))
  dataSource.setServerName(ContentApiProperties.get("META_SERVER"))
  dataSource.setPortNumber(ContentApiProperties.getInt("META_PORT"))
  dataSource.setInitialConnections(ContentApiProperties.getInt("META_INITIAL_CONNECTIONS"))
  dataSource.setMaxConnections(ContentApiProperties.getInt("META_MAX_CONNECTIONS"))
  dataSource.setCurrentSchema(ContentApiProperties.get("META_SCHEMA"))

  val elasticClient = ElasticClient.remote(
    ImmutableSettings.settingsBuilder().put("cluster.name", ContentApiProperties.SearchClusterName).build(),
    ElasticsearchClientUri(s"elasticsearch://ContentApiProperties.SearchHost:ContentApiProperties.SearchPort")
  )

  val contentRepository = new ContentRepository
  val elasticContentSearch = new ElasticContentSearch
  val elasticContentIndex = new ElasticContentIndex
}
