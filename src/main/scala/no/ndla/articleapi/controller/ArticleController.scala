/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.controller

import no.ndla.articleapi.model.api.{Article, Error, SearchResult}
import no.ndla.articleapi.model.domain.Sort
import no.ndla.articleapi.service.ReadService
import no.ndla.articleapi.service.search.SearchService
import org.scalatra.swagger.{Swagger, SwaggerSupport}

import scala.util.Try

trait ArticleController {
  this: ReadService with SearchService =>
  val articleController: ArticleController

  class ArticleController(implicit val swagger: Swagger) extends NdlaController with SwaggerSupport {
    protected val applicationDescription = "API for accessing images from ndla.no."

    val getAllArticles =
      (apiOperation[List[SearchResult]]("getAllArticles")
        summary "Show all articles"
        notes "Shows all articles. You can search it too."
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key. May be omitted to access api anonymously, but rate limiting applies on anonymous access."),
        queryParam[Option[String]]("query").description("Return only articles with content matching the specified query."),
        queryParam[Option[String]]("language").description("The ISO 639-1 language code describing language used in query-params."),
        queryParam[Option[String]]("license").description("Return only articles with provided license."),
        queryParam[Option[Int]]("page").description("The page number of the search hits to display."),
        queryParam[Option[Int]]("page-size").description("The number of search hits to display for each page."),
        queryParam[Option[String]]("sort").description(
          """The sorting used on results.
           Default is by -relevance (desc) when querying.
           When browsing, the default is title (asc).
           The following are supported: relevance, -relevance, title, -title, lastUpdated, -lastUpdated""".stripMargin)
        ))

    val getArticleById =
      (apiOperation[List[Article]]("getArticleById")
        summary "Show article with a specified Id"
        notes "Shows the article for the specified id."
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key. May be omitted to access api anonymously, but rate limiting applies on anonymous access."),
        pathParam[Long]("article_id").description("Id of the article that is to be returned")
        ))

    get("/", operation(getAllArticles)) {
      val query = paramOrNone("query")
      val language = paramOrNone("language")
      val license = paramOrNone("license")
      val sort = paramOrNone("sort")
      val pageSize = paramOrNone("page-size").flatMap(ps => Try(ps.toInt).toOption)
      val page = paramOrNone("page").flatMap(idx => Try(idx.toInt).toOption)

      query match {
        case Some(q) => searchService.matchingQuery(
          query = q.toLowerCase().split(" ").map(_.trim),
          language = language,
          license = license,
          page = page,
          pageSize = pageSize,
          sort = Sort.valueOf(sort).getOrElse(Sort.ByRelevanceDesc))

        case None => searchService.all(
          language = language,
          license = license,
          page = page,
          pageSize = pageSize,
          sort = Sort.valueOf(sort).getOrElse(Sort.ByTitleAsc))
      }
    }

    get("/:article_id", operation(getArticleById)) {
      val articleId = long("article_id")

      readService.withId(articleId) match {
        case Some(image) => image
        case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"No article with id $articleId found"))
      }

    }
  }

}