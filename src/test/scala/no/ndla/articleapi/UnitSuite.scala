/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi

import org.scalatest._
import org.scalatest.mockito.MockitoSugar

//Own tag for elasticsearch tests that need a running elasticsearch instance outside the test (e.g. in the docker container) on port 9200
//The tag is not named no.ndla.articleapi.ESIntegrationTest due to the sbt test-only runner getting confused when it
// matches package and will not run the tests as it then looks for package with given name as if it is a FunSuite instead of Tag.
object ESIntegrationTest extends Tag("no.ndla.tag.ESIntegrationTest")

abstract class UnitSuite extends FunSuite with Matchers with OptionValues with Inside with Inspectors with MockitoSugar with BeforeAndAfterEach with BeforeAndAfterAll {

  setEnv("NDLA_ENVIRONMENT", "local")
  setEnv("ENABLE_JOUBEL_H5P_OEMBED", "true")

  setEnv("SEARCH_SERVER", "some-server")
  setEnv("SEARCH_REGION", "some-region")
  setEnv("RUN_WITH_SIGNED_SEARCH_REQUESTS", "false")

  setEnv("MIGRATION_HOST", "some-host")
  setEnv("MIGRATION_USER", "some-user")
  setEnv("MIGRATION_PASSWORD", "some-password")

  setEnv("NDLA_BRIGHTCOVE_ACCOUNT_ID", "some-account-id")
  setEnv("NDLA_BRIGHTCOVE_PLAYER_ID", "some-player-id")

  def setEnv(key: String, value: String) = env.put(key, value)

  def setEnvIfAbsent(key: String, value: String) = env.putIfAbsent(key, value)

  private def env = {
    val field = System.getenv().getClass.getDeclaredField("m")
    field.setAccessible(true)
    field.get(System.getenv()).asInstanceOf[java.util.Map[java.lang.String, java.lang.String]]
  }
}
