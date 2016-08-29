/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.controller

import no.ndla.articleapi.ComponentRegistry.{contentRepository, elasticContentSearch}
import no.ndla.articleapi.model.Error._
import no.ndla.articleapi.model.{ContentInformation, ContentSummary, Error}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.swagger.{Swagger, SwaggerSupport}

import scala.util.Try

trait ContentController {
  val contentController: ContentController

  class ContentController(implicit val swagger: Swagger) extends NdlaController with SwaggerSupport {

    protected implicit override val jsonFormats: Formats = DefaultFormats

    protected val applicationDescription = "API for accessing images from ndla.no."

    val getAllContent =
      (apiOperation[List[ContentSummary]]("getAllContent")
        summary "Show all content"
        notes "Shows all the content. You can search it too."
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key. May be omitted to access api anonymously, but rate limiting applies on anonymous access."),
        queryParam[Option[String]]("tags").description("Return only content with submitted tag. Multiple tags may be entered comma separated, and will give results matching either one of them."),
        queryParam[Option[String]]("language").description("The ISO 639-1 language code describing language used in query-params."),
        queryParam[Option[String]]("license").description("Return only content with provided license."),
        queryParam[Option[Int]]("page").description("The page number of the search hits to display."),
        queryParam[Option[Int]]("page-size").description("The number of search hits to display for each page.")
        ))

    val getContentById =
      (apiOperation[List[ContentInformation]]("getContentById")
        summary "Show content for a specified Id"
        notes "Shows the content for the specified id."
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key. May be omitted to access api anonymously, but rate limiting applies on anonymous access."),
        pathParam[String]("content_id").description("Id of the content that is to be returned")
        ))

    get("/", operation(getAllContent)) {
      val query = params.get("query")
      val language = params.get("language")
      val license = params.get("license")
      val pageSize = params.get("page-size").flatMap(ps => Try(ps.toInt).toOption)
      val page = params.get("page").flatMap(idx => Try(idx.toInt).toOption)
      logger.info("GET / with params query='{}', language={}, license={}, page={}, page-size={}", query, language, license, page, pageSize)

      query match {
        case Some(query) => elasticContentSearch.matchingQuery(
          query = query.toLowerCase().split(" ").map(_.trim),
          language = language,
          license = license,
          page = page,
          pageSize = pageSize)

        case None => elasticContentSearch.all(license = license, page = page, pageSize = pageSize)
      }
    }

    get("/:content_id", operation(getContentById)) {
      val contentId = params("content_id")
      logger.info("GET /{}", contentId)

      if (contentId.forall(_.isDigit)) {
        contentRepository.withId(contentId) match {
          case Some(image) => image
          case None => halt(status = 404, body = Error(NOT_FOUND, s"No content with id $contentId found"))
        }
      } else {
        halt(status = 404, body = Error(NOT_FOUND, s"No content with id $contentId found"))
      }
    }
  }

}