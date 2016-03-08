package no.ndla.contentapi.integration

import no.ndla.contentapi.ContentApiProperties
import no.ndla.contentapi.business.{ContentIndex, ContentSearch, ContentData}
import org.postgresql.ds.PGPoolingDataSource

object AmazonIntegration {

  private val datasource = new PGPoolingDataSource()
  datasource.setUser(ContentApiProperties.get("META_USER_NAME"))
  datasource.setPassword(ContentApiProperties.get("META_PASSWORD"))
  datasource.setDatabaseName(ContentApiProperties.get("META_RESOURCE"))
  datasource.setServerName(ContentApiProperties.get("META_SERVER"))
  datasource.setPortNumber(ContentApiProperties.getInt("META_PORT"))
  datasource.setInitialConnections(ContentApiProperties.getInt("META_INITIAL_CONNECTIONS"))
  datasource.setMaxConnections(ContentApiProperties.getInt("META_MAX_CONNECTIONS"))
  datasource.setCurrentSchema(ContentApiProperties.get("META_SCHEMA"))

  def getContentData(): ContentData = {
    new PostgresData(datasource)
  }

  def getContentSearch(): ContentSearch = {
    new ElasticContentSearch(
      ContentApiProperties.SearchClusterName,
      ContentApiProperties.SearchHost,
      ContentApiProperties.SearchPort)
  }
  def getContentIndex(): ContentIndex = {
    new ElasticContentIndex(
      ContentApiProperties.SearchClusterName,
      ContentApiProperties.SearchHost,
      ContentApiProperties.SearchPort)


  }


}
