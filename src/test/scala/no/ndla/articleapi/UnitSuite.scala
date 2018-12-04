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

abstract class UnitSuite
    extends FunSuite
    with Matchers
    with OptionValues
    with Inside
    with Inspectors
    with MockitoSugar
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with PrivateMethodTester {

  setEnv("NDLA_ENVIRONMENT", "local")

  setEnv("SEARCH_SERVER", "some-server")
  setEnv("SEARCH_REGION", "some-region")
  setEnv("RUN_WITH_SIGNED_SEARCH_REQUESTS", "false")

  setEnv("AUDIO_API_HOST", "localhost:30014")
  setEnv("IMAGE_API_HOST", "localhost:30001")
  setEnv("DRAFT_API_HOST", "localhost:30022")

  setEnvIfAbsent("MIGRATION_HOST", "some-host")
  setEnvIfAbsent("MIGRATION_USER", "some-user")
  setEnvIfAbsent("MIGRATION_PASSWORD", "some-password")

  setEnv("NDLA_BRIGHTCOVE_ACCOUNT_ID", "some-account-id")
  setEnv("NDLA_BRIGHTCOVE_PLAYER_ID", "some-player-id")
  setEnv("SEARCH_INDEX_NAME", "article-integration-test-index")
  setEnv("CONCEPT_SEARCH_INDEX_NAME", "concept-integration-test-index")

  def setEnv(key: String, value: String) = env.put(key, value)

  def setEnvIfAbsent(key: String, value: String) = env.putIfAbsent(key, value)

  private def env = {
    val field = System.getenv().getClass.getDeclaredField("m")
    field.setAccessible(true)
    field.get(System.getenv()).asInstanceOf[java.util.Map[java.lang.String, java.lang.String]]
  }
}
