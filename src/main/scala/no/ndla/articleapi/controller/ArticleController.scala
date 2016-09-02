/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.controller

import no.ndla.articleapi.ComponentRegistry.{articleRepository, elasticContentSearch}
import no.ndla.articleapi.model.Error._
import no.ndla.articleapi.model.{ArticleInformation, ArticleSummary, Error}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.swagger.{Swagger, SwaggerSupport}

import scala.util.Try

trait ArticleController {
  val articleController: ArticleController

  class ArticleController(implicit val swagger: Swagger) extends NdlaController with SwaggerSupport {

    protected implicit override val jsonFormats: Formats = DefaultFormats

    protected val applicationDescription = "API for accessing images from ndla.no."

    val getAllArticles =
      (apiOperation[List[ArticleSummary]]("getAllArticles")
        summary "Show all articles"
        notes "Shows all articles. You can search it too."
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key. May be omitted to access api anonymously, but rate limiting applies on anonymous access."),
        queryParam[Option[String]]("query").description("Return only articles with content matching the specified query."),
        queryParam[Option[String]]("language").description("The ISO 639-1 language code describing language used in query-params."),
        queryParam[Option[String]]("license").description("Return only articles with provided license."),
        queryParam[Option[Int]]("page").description("The page number of the search hits to display."),
        queryParam[Option[Int]]("page-size").description("The number of search hits to display for each page.")
        ))

    val getArticleById =
      (apiOperation[List[ArticleInformation]]("getArticleById")
        summary "Show article with a specified Id"
        notes "Shows the article for the specified id."
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key. May be omitted to access api anonymously, but rate limiting applies on anonymous access."),
        pathParam[String]("article_id").description("Id of the article that is to be returned")
        ))

    get("/", operation(getAllArticles)) {
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

    get("/:article_id", operation(getArticleById)) {
      val articleId = params("article_id")
      logger.info("GET /{}", articleId)

      if (articleId.forall(_.isDigit)) {
        articleRepository.withId(articleId) match {
          case Some(image) => image
          case None => halt(status = 404, body = Error(NOT_FOUND, s"No article with id $articleId found"))
        }
      } else {
        halt(status = 404, body = Error(NOT_FOUND, s"No article with id $articleId found"))
      }
    }
  }

}