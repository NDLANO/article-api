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
  val DEFAULT_PAGE_SIZE = 12
  val MAX_PAGE_SIZE = 548

  ArticleApiProperties.setProperties(Map(
    "CONTACT_EMAIL" -> Some("someone@somewhere.earth"),
    "HOST_ADDR" -> Some("localhost"),
    "NDLA_ENVIRONMENT" -> Some("local"),

    "DB_USER_NAME" -> Some("user"),
    "DB_PASSWORD" -> Some("password"),
    "DB_RESOURCE" -> Some("dbresource"),
    "DB_SERVER" -> Some("dbserver"),
    "DB_PORT" -> Some("1"),
    "DB_SCHEMA" -> Some("dbschema"),

    "SEARCH_SERVER" -> Some("search-server"),
    "RUN_WITH_SIGNED_SEARCH_REQUESTS" -> Some("false"),
    "SEARCH_REGION" -> Some("some-region"),
    "SEARCH_INDEX" -> Some("articles"),
    "SEARCH_DOCUMENT" -> Some("article"),
    "SEARCH_DEFAULT_PAGE_SIZE" -> Some(s"$DEFAULT_PAGE_SIZE"),
    "SEARCH_MAX_PAGE_SIZE" -> Some(s"$MAX_PAGE_SIZE"),
    "INDEX_BULK_SIZE" -> Some("500"),

    "AMAZON_BASE_URL" -> Some("http://amazon"),
    "STORAGE_NAME" -> Some("test.storage"),
    "NDLA_BRIGHTCOVE_ACCOUNT_ID" -> Some("0123456789"),
    "NDLA_BRIGHTCOVE_PLAYER_ID" -> Some("qwerty"),
    "NDLA_API_URL" -> Some("http://localhost")
  ))
}
