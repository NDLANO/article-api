/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi

import javax.sql.DataSource

import no.ndla.network.secrets.PropertyKeys
import org.postgresql.ds.PGPoolingDataSource

abstract class IntegrationSuite extends UnitSuite {

  setEnv(PropertyKeys.MetaUserNameKey, "postgres")
  setEnv(PropertyKeys.MetaPasswordKey, "hemmelig")
  setEnv(PropertyKeys.MetaResourceKey, "postgres")
  setEnv(PropertyKeys.MetaServerKey, "127.0.0.1")
  setEnv(PropertyKeys.MetaPortKey, "5432")
  setEnv(PropertyKeys.MetaSchemaKey, "articleapitest")


  def getDataSource: DataSource = {
    val datasource = new PGPoolingDataSource()
    datasource.setUser(ArticleApiProperties.MetaUserName)
    datasource.setPassword(ArticleApiProperties.MetaPassword)
    datasource.setDatabaseName(ArticleApiProperties.MetaResource)
    datasource.setServerName(ArticleApiProperties.MetaServer)
    datasource.setPortNumber(ArticleApiProperties.MetaPort)
    datasource.setInitialConnections(ArticleApiProperties.MetaInitialConnections)
    datasource.setMaxConnections(ArticleApiProperties.MetaMaxConnections)
    datasource.setCurrentSchema(ArticleApiProperties.MetaSchema)
    datasource
  }
}
