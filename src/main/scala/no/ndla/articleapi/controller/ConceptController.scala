/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.controller

import no.ndla.articleapi.ArticleApiProperties
import no.ndla.articleapi.auth.Role
import no.ndla.articleapi.model.api.{Concept, ConceptSearchParams, ConceptSearchResult, Error, NewConcept, UpdatedConcept}
import no.ndla.articleapi.model.domain.{Language, Sort}
import no.ndla.articleapi.service.{ReadService, WriteService}
import no.ndla.articleapi.service.search.ConceptSearchService
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.NotFound
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}

import scala.util.{Failure, Success}

trait ConceptController {
  this: ReadService with WriteService with ConceptSearchService with Role =>
  val conceptController: ConceptController

  class ConceptController(implicit val swagger: Swagger) extends NdlaController with SwaggerSupport {
    protected implicit override val jsonFormats: Formats = DefaultFormats
    protected val applicationDescription = "API for accessing concepts from ndla.no."

    registerModel[Error]()

    val response400 = ResponseMessage(400, "Validation Error", Some("ValidationError"))
    val response403 = ResponseMessage(403, "Access Denied", Some("Error"))
    val response404 = ResponseMessage(404, "Not found", Some("Error"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    val getAllConcepts =
      (apiOperation[ConceptSearchResult]("getAllConcepts")
        summary "Show all concepts"
        notes "Shows all concepts. You can search it too."
        parameters(
          headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
          queryParam[Option[String]]("query").description("Return only concepts with content matching the specified query."),
          queryParam[Option[String]]("ids").description("Return only concepts that have one of the provided ids. To provide multiple ids, separate by comma (,)."),
          queryParam[Option[String]]("language").description("The ISO 639-1 language code describing language used in query-params."),
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

    val getConceptById =
      (apiOperation[String]("getConceptById")
        summary "Show concept with a specified Id"
        notes "Shows the concept for the specified id."
        parameters(
          headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
          pathParam[Long]("concept_id").description("Id of the concept that is to be returned")
        )
        authorizations "oauth2"
        responseMessages(response404, response500))

    val getAllConceptsPost =
      (apiOperation[ConceptSearchResult]("searchConcepts")
        summary "Show all concepts"
        notes "Shows all concepts. You can search it too."
        parameters(
          headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id"),
          bodyParam[ConceptSearchParams]
        )
        authorizations "oauth2"
        responseMessages(response400, response500))

    private def search(query: Option[String],
                       sort: Option[Sort.Value],
                       language: String,
                       page: Int,
                       pageSize: Int,
                       idList: List[Long],
                       fallback: Boolean) = {
      query match {
        case Some(q) => conceptSearchService.matchingQuery(
          query = q,
          withIdIn = idList,
          searchLanguage = language,
          page = page,
          pageSize = pageSize,
          sort = sort.getOrElse(Sort.ByRelevanceDesc),
          fallback = fallback
        )

        case None => conceptSearchService.all(
          withIdIn = idList,
          language = language,
          page = page,
          pageSize = pageSize,
          sort = sort.getOrElse(Sort.ByTitleAsc),
          fallback = fallback
        )
      }

    }

    get("/", operation(getAllConcepts)) {
      val query = paramOrNone("query")
      val sort = Sort.valueOf(paramOrDefault("sort", ""))
      val language = paramOrDefault("language", Language.NoLanguage)
      val pageSize = intOrDefault("page-size", ArticleApiProperties.DefaultPageSize)
      val page = intOrDefault("page", 1)
      val idList = paramAsListOfLong("ids")
      val fallback = booleanOrDefault("fallback", default = false)

      search(query, sort, language, page, pageSize, idList, fallback)
    }

    post("/search/", operation(getAllConceptsPost)) {
      val searchParams = extract[ConceptSearchParams](request.body)

      val query = searchParams.query
      val sort = Sort.valueOf(searchParams.sort.getOrElse(""))
      val language = searchParams.language.getOrElse(Language.NoLanguage)
      val pageSize = searchParams.pageSize.getOrElse(ArticleApiProperties.DefaultPageSize)
      val page = searchParams.page.getOrElse(1)
      val idList = searchParams.idList
      val fallback = booleanOrDefault("fallback", default = false)

      search(query, sort, language, page, pageSize, idList, fallback)
    }

    get("/:id", operation(getConceptById)) {
      val conceptId = long("id")
      val language = paramOrDefault("language", Language.NoLanguage)

      readService.conceptWithId(conceptId, language) match {
        case Some(concept) => concept
        case None => NotFound(body = Error(Error.NOT_FOUND, s"No concept with id $conceptId found"))
      }
    }

  }
}
