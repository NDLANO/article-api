/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi

import org.scalatest._
import org.scalatest.mock.MockitoSugar


abstract class UnitSuite extends FunSuite with Matchers with OptionValues with Inside with Inspectors with MockitoSugar with BeforeAndAfterEach with BeforeAndAfterAll {

  ArticleApiProperties.setProperties(Map(
    "NDLA_ENVIRONMENT" -> Some("local"),

    "NDLA_BRIGHTCOVE_ACCOUNT_ID" -> Some("0123456789"),
    "NDLA_BRIGHTCOVE_PLAYER_ID" -> Some("qwerty"),

    "MIGRATION_HOST" -> Some("article-api"),
    "MIGRATION_USER" -> Some("user"),
    "MIGRATION_PASSWORD" -> Some("password")
  ))
}
