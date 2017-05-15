/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.controller

import no.ndla.articleapi.ArticleApiProperties.RoleWithWriteAccess
import no.ndla.articleapi.auth.Role
import no.ndla.articleapi.model.api._
import no.ndla.articleapi.model.domain.Sort
import no.ndla.articleapi.service.{ReadService, WriteService}
import no.ndla.articleapi.service.search.SearchService
import org.json4s.native.Serialization.read
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.{Created, NotFound, Ok}
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}

import scala.util.{Failure, Success, Try}

trait ArticleController {
  this: ReadService with WriteService with SearchService with Role =>
  val articleController: ArticleController

  class ArticleController(implicit val swagger: Swagger) extends NdlaController with SwaggerSupport {
    protected implicit override val jsonFormats: Formats = DefaultFormats
    protected val applicationDescription = "API for accessing articles from ndla.no."

    // Additional models used in error responses
    registerModel[ValidationError]()
    registerModel[Error]()

    val response400 = ResponseMessage(400, "Validation Error", Some("ValidationError"))
    val response403 = ResponseMessage(403, "Access Denied", Some("Error"))
    val response404 = ResponseMessage(404, "Not found", Some("Error"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    val getAllArticles =
      (apiOperation[List[SearchResult]]("getAllArticles")
        summary "Show all articles"
        notes "Shows all articles. You can search it too."
        parameters(
          headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
          headerParam[Option[String]]("app-key").description("Your app-key. May be omitted to access api anonymously, but rate limiting applies on anonymous access."),
          queryParam[Option[String]]("query").description("Return only articles with content matching the specified query."),
          queryParam[Option[String]]("ids").description("Return only articles that have one of the provided ids. To provide multiple ids, separate by comma (,)."),
          queryParam[Option[String]]("language").description("The ISO 639-1 language code describing language used in query-params."),
          queryParam[Option[String]]("license").description("Return only articles with provided license."),
          queryParam[Option[Int]]("page").description("The page number of the search hits to display."),
          queryParam[Option[Int]]("page-size").description("The number of search hits to display for each page."),
          queryParam[Option[String]]("sort").description(
            """The sorting used on results.
             Default is by -relevance (desc) when querying.
             When browsing, the default is title (asc).
             The following are supported: relevance, -relevance, title, -title, lastUpdated, -lastUpdated, id, -id""".stripMargin)
        )
        authorizations "oauth2"
        responseMessages(response500))

    val getArticleById =
      (apiOperation[List[Article]]("getArticleById")
        summary "Show article with a specified Id"
        notes "Shows the article for the specified id."
        parameters(
          headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
          headerParam[Option[String]]("app-key").description("Your app-key. May be omitted to access api anonymously, but rate limiting applies on anonymous access."),
          pathParam[Long]("article_id").description("Id of the article that is to be returned")
        )
        authorizations "oauth2"
        responseMessages(response404, response500))

    val newArticle =
      (apiOperation[Article]("newArticle")
        summary "Create a new article"
        notes "Creates a new article"
        parameters(
          headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id"),
          headerParam[Option[String]]("app-key").description("Your app-key"),
          bodyParam[NewArticle]
        )
        authorizations "oauth2"
        responseMessages(response400, response403, response500))

    val updateArticle =
      (apiOperation[Article]("updateArticle")
        summary "Update an existing article"
        notes "Update an existing article"
        parameters(
          headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id"),
          headerParam[Option[String]]("app-key").description("Your app-key"),
          pathParam[Long]("article_id").description("Id of the article that is to be updated"),
          bodyParam[UpdatedArticle]
        )
        authorizations "oauth2"
        responseMessages(response400, response403, response404, response500))

    val getTags =
      (apiOperation[List[ArticleTag]]("getTags")
        summary "Retrieves a list of all previously used tags in articles"
        notes "Retrieves a list of all previously used tags in articles"
        parameters(
        queryParam[Option[Int]]("size").description("Limit the number of results to this many elements"),
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."))
        responseMessages response500
        authorizations "oauth2")

    get("/tags/", operation(getTags)) {
      val defaultSize = 20
      val size = intOrDefault("size", defaultSize) match {
        case toSmall if toSmall < 1 => defaultSize
        case x => x
      }

      readService.getNMostUsedTags(size)
    }

    get("/", operation(getAllArticles)) {
      val query = paramOrNone("query")
      val language = paramOrNone("language")
      val license = paramOrNone("license")
      val sort = paramOrNone("sort")
      val pageSize = paramOrNone("page-size").flatMap(ps => Try(ps.toInt).toOption)
      val page = paramOrNone("page").flatMap(idx => Try(idx.toInt).toOption)
      val idList = paramAsListOfLong("ids")

      query match {
        case Some(q) => searchService.matchingQuery(
          query = q.toLowerCase().split(" ").map(_.trim),
          withIdIn = idList,
          language = language,
          license = license,
          page = page,
          pageSize = pageSize,
          sort = Sort.valueOf(sort).getOrElse(Sort.ByRelevanceDesc))

        case None => searchService.all(
          withIdIn = idList,
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
        case None => NotFound(body = Error(Error.NOT_FOUND, s"No article with id $articleId found"))
      }
    }

    post("/", operation(newArticle)) {
      authRole.assertHasRole(RoleWithWriteAccess)

      val newArticle = extract[NewArticle](request.body)
      val article = writeService.newArticle(newArticle)
      Created(body=article)
    }

    patch("/:article_id", operation(updateArticle)) {
      authRole.assertHasRole(RoleWithWriteAccess)

      val articleId = long("article_id")
      val updatedArticle = extract[UpdatedArticle](request.body)
      writeService.updateArticle(articleId, updatedArticle) match {
        case Success(article) => Ok(body=article)
        case Failure(exception) => errorHandler(exception)
      }
    }

    def extract[T](json: String)(implicit mf: scala.reflect.Manifest[T]): T = {
      Try {
        read[T](json)
      } match {
        case Failure(e) => {
          logger.error(e.getMessage, e)
          throw new ValidationException(errors=Seq(ValidationMessage("body", e.getMessage)))
        }
        case Success(data) => data
      }
    }

  }
}