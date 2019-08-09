/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi

import com.zaxxer.hikari.HikariDataSource
import no.ndla.network.secrets.PropertyKeys
import no.ndla.articleapi.integration.DataSource.getHikariDataSource

import scala.util.Try

abstract class IntegrationSuite extends UnitSuite {

  setEnv(PropertyKeys.MetaUserNameKey, "postgres")
  setEnvIfAbsent(PropertyKeys.MetaPasswordKey, "hemmelig")
  setEnv(PropertyKeys.MetaResourceKey, "postgres")
  setEnv(PropertyKeys.MetaServerKey, "127.0.0.1")
  setEnv(PropertyKeys.MetaPortKey, "5432")
  setEnv(PropertyKeys.MetaSchemaKey, "articleapitest")

  val testDataSource: Try[HikariDataSource] = Try(getHikariDataSource)
}
