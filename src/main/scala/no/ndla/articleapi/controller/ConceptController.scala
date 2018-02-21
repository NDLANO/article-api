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
import org.scalatra.util.NotNothing
import scala.util.{Failure, Success}

trait ConceptController {
  this: ReadService with WriteService with ConceptSearchService with Role =>
  val conceptController: ConceptController

  class ConceptController(implicit val swagger: Swagger) extends NdlaController with SwaggerSupport {
    protected implicit override val jsonFormats: Formats = DefaultFormats
    protected val applicationDescription = "Services for accessing concepts"

    registerModel[Error]()

    val response400 = ResponseMessage(400, "Validation Error", Some("ValidationError"))
    val response403 = ResponseMessage(403, "Access Denied", Some("Error"))
    val response404 = ResponseMessage(404, "Not found", Some("Error"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    private val correlationId = Param("X-Correlation-ID","User supplied correlation-id. May be omitted.")
    private val query = Param("query","Return only concepts with content matching the specified query.")
    private val language = Param("language", "The ISO 639-1 language code describing language.")
    private val sort = Param("sort",
      """The sorting used on results.
             The following are supported: relevance, -relevance, title, -title, lastUpdated, -lastUpdated, id, -id.
             Default is by -relevance (desc) when query is set, and id (asc) when query is empty.""".stripMargin)
    private val pageNo = Param("page","The page number of the search hits to display.")
    private val pageSize = Param("page-size","The number of search hits to display for each page.")
    private val conceptId = Param("concept_id","Id of the concept that is to be fecthed")
    private val conceptIds = Param("ids","Return only concepts that have one of the provided ids. To provide multiple ids, separate by comma (,).")
    private val fallback = Param("fallback", "Fallback to existing language if language is specified.")


    private def asQueryParam[T: Manifest: NotNothing](param: Param) = queryParam[T](param.paramName).description(param.description)
    private def asHeaderParam[T: Manifest: NotNothing](param: Param) = headerParam[T](param.paramName).description(param.description)
    private def asPathParam[T: Manifest: NotNothing](param: Param) = pathParam[T](param.paramName).description(param.description)

    val getAllConcepts =
      (apiOperation[ConceptSearchResult]("getAllConcepts")
        summary "Find concepts"
        notes "Shows all concepts. You can search it too."
        parameters(
          asHeaderParam[Option[String]](correlationId),
          asQueryParam[Option[String]](query),
          asQueryParam[Option[String]](conceptIds),
          asQueryParam[Option[String]](language),
          asQueryParam[Option[Int]](pageNo),
          asQueryParam[Option[Int]](pageSize),
          asQueryParam[Option[String]](sort)
        )
        authorizations "oauth2"
        responseMessages(response500))

    get("/", operation(getAllConcepts)) {
      val query = paramOrNone(this.query.paramName)
      val sort = Sort.valueOf(paramOrDefault(this.sort.paramName, ""))
      val language = paramOrDefault(this.language.paramName, Language.NoLanguage)
      val pageSize = intOrDefault(this.pageSize.paramName, ArticleApiProperties.DefaultPageSize)
      val page = intOrDefault(this.pageNo.paramName, 1)
      val idList = paramAsListOfLong(this.conceptIds.paramName)
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)

      search(query, sort, language, page, pageSize, idList, fallback)
    }

    val getAllConceptsPost =
      (apiOperation[ConceptSearchResult]("searchConcepts")
        summary "Find concepts"
        notes "Shows all concepts. You can search it too."
        parameters(
          asHeaderParam[Option[String]](correlationId),
          bodyParam[ConceptSearchParams]
        )
        authorizations "oauth2"
        responseMessages(response400, response500))

    post("/search/", operation(getAllConceptsPost)) {
      val searchParams = extract[ConceptSearchParams](request.body)

      val query = searchParams.query
      val sort = Sort.valueOf(searchParams.sort.getOrElse(""))
      val language = searchParams.language.getOrElse(Language.NoLanguage)
      val pageSize = searchParams.pageSize.getOrElse(ArticleApiProperties.DefaultPageSize)
      val page = searchParams.page.getOrElse(1)
      val idList = searchParams.idList
      val fallback = searchParams.fallback.getOrElse(false)

      search(query, sort, language, page, pageSize, idList, fallback)
    }

    private def search(query: Option[String],
                       sort: Option[Sort.Value],
                       language: String,
                       page: Int,
                       pageSize: Int,
                       idList: List[Long],
                       fallback: Boolean) = {
      val result = query match {
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
          sort = sort.getOrElse(Sort.ByIdAsc),
          fallback = fallback
        )
      }

      result match {
        case Success(searchResult) => searchResult
        case Failure(ex) => errorHandler(ex)
      }

    }

    val getConceptById =
      (apiOperation[String]("getConceptById")
        summary "Fetch specified concept"
        notes "Shows the concept for the specified id."
        parameters(
          asHeaderParam[Option[String]](correlationId),
          asPathParam[Long](conceptId)
        )
        authorizations "oauth2"
        responseMessages(response404, response500))

    get("/:concept_id", operation(getConceptById)) {
      val conceptId = long(this.conceptId.paramName)
      val language = paramOrDefault(this.language.paramName, Language.NoLanguage)
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)

      readService.conceptWithId(conceptId, language) match {
        case Some(concept) => concept
        case None => NotFound(body = Error(Error.NOT_FOUND, s"No concept with id $conceptId found"))
      }
    }

  }
}
