/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.controller

import no.ndla.articleapi.ArticleApiProperties
import no.ndla.articleapi.ArticleApiProperties.{
  DefaultPageSize,
  MaxPageSize,
  ElasticSearchIndexMaxResultWindow,
  ElasticSearchScrollKeepAlive
}
import no.ndla.articleapi.auth.Role
import no.ndla.articleapi.model.api.{ConceptSearchParams, ConceptSearchResult, Error}
import no.ndla.articleapi.model.domain.{Language, Sort}
import no.ndla.articleapi.service.search.{ConceptSearchService, SearchConverterService, SearchService}
import no.ndla.articleapi.service.{ReadService, WriteService}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.Ok
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}
import org.scalatra.util.NotNothing

import scala.util.{Failure, Success}

trait ConceptController {
  this: ReadService with WriteService with ConceptSearchService with SearchConverterService with Role =>
  val conceptController: ConceptController

  class ConceptController(implicit val swagger: Swagger) extends NdlaController with SwaggerSupport {
    protected implicit override val jsonFormats: Formats = DefaultFormats
    protected val applicationDescription = "Services for accessing concepts"

    registerModel[Error]()

    val response400 = ResponseMessage(400, "Validation Error", Some("ValidationError"))
    val response403 = ResponseMessage(403, "Access Denied", Some("Error"))
    val response404 = ResponseMessage(404, "Not found", Some("Error"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    private val correlationId =
      Param[Option[String]]("X-Correlation-ID", "User supplied correlation-id. May be omitted.")
    private val query =
      Param[Option[String]]("query", "Return only concepts with content matching the specified query.")
    private val language = Param[Option[String]]("language", "The ISO 639-1 language code describing language.")
    private val sort = Param[Option[String]](
      "sort",
      s"""The sorting used on results.
             The following are supported: ${Sort.values.mkString(", ")}.
             Default is by -relevance (desc) when query is set, and id (asc) when query is empty.""".stripMargin
    )
    private val pageNo = Param[Option[Int]]("page", "The page number of the search hits to display.")
    private val pageSize = Param[Option[Int]](
      "page-size",
      s"The number of search hits to display for each page. Defaults to $DefaultPageSize and max is $MaxPageSize.")
    private val conceptId = Param[Long]("concept_id", "Id of the concept that is to be fetched.")
    private val conceptIds = Param[Option[Seq[Long]]](
      "ids",
      "Return only concepts that have one of the provided ids. To provide multiple ids, separate by comma (,).")
    private val fallback = Param[Option[Boolean]]("fallback", "Fallback to existing language if language is specified.")
    private val scrollId = Param[Option[String]](
      "search-context",
      s"""A search context retrieved from the response header of a previous search.
         |If search-context is specified, all other query parameters, except '${this.language.paramName}' and '${this.fallback.paramName}' are ignored
         |For the rest of the parameters the original search of the search-context is used.
         |The search context may change between scrolls. Always use the most recent one (The context if unused dies after $ElasticSearchScrollKeepAlive).
         |Used to enable scrolling past $ElasticSearchIndexMaxResultWindow results.
      """.stripMargin
    )

    private def asQueryParam[T: Manifest: NotNothing](param: Param[T]) =
      queryParam[T](param.paramName).description(param.description)

    private def asHeaderParam[T: Manifest: NotNothing](param: Param[T]) =
      headerParam[T](param.paramName).description(param.description)

    private def asPathParam[T: Manifest: NotNothing](param: Param[T]) =
      pathParam[T](param.paramName).description(param.description)

    /**
      * Does a scroll with [[ConceptSearchService]]
      * If no scrollId is specified execute the function @orFunction in the second parameter list.
      *
      * @param orFunction Function to execute if no scrollId in parameters (Usually searching)
      * @return A Try with scroll result, or the return of the orFunction (Usually a try with a search result).
      */
    private def scrollSearchOr(orFunction: => Any): Any = {
      val language = paramOrDefault(this.language.paramName, Language.AllLanguages)
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)

      paramOrNone(this.scrollId.paramName) match {
        case Some(scroll) =>
          conceptSearchService.scroll(scroll, language, fallback) match {
            case Success(scrollResult) =>
              val responseHeader = scrollResult.scrollId.map(i => this.scrollId.paramName -> i).toMap
              Ok(searchConverterService.asApiConceptSearchResult(scrollResult), headers = responseHeader)
            case Failure(ex) => errorHandler(ex)
          }
        case None => orFunction
      }
    }

    get(
      "/",
      operation(
        apiOperation[ConceptSearchResult]("getAllConcepts")
          summary "Find concepts"
          description "Shows all concepts. You can search it too."
          parameters (
            asHeaderParam(correlationId),
            asQueryParam(query),
            asQueryParam(conceptIds),
            asQueryParam(language),
            asQueryParam(pageNo),
            asQueryParam(pageSize),
            asQueryParam(sort),
            asQueryParam(fallback),
            asQueryParam(scrollId)
        )
          responseMessages response500)
    ) {
      scrollSearchOr {
        val query = paramOrNone(this.query.paramName)
        val sort = Sort.valueOf(paramOrDefault(this.sort.paramName, ""))
        val language = paramOrDefault(this.language.paramName, Language.NoLanguage)
        val pageSize = intOrDefault(this.pageSize.paramName, ArticleApiProperties.DefaultPageSize)
        val page = intOrDefault(this.pageNo.paramName, 1)
        val idList = paramAsListOfLong(this.conceptIds.paramName)
        val fallback = booleanOrDefault(this.fallback.paramName, default = false)

        search(query, sort, language, page, pageSize, idList, fallback)
      }
    }

    post(
      "/search/",
      operation(
        apiOperation[ConceptSearchResult]("searchConcepts")
          summary "Find concepts."
          description "Searches all concepts."
          parameters (
            asHeaderParam(correlationId),
            asQueryParam(scrollId),
            bodyParam[ConceptSearchParams]
        )
          responseMessages (response400, response500))
    ) {
      scrollSearchOr {
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
    }

    private def search(query: Option[String],
                       sort: Option[Sort.Value],
                       language: String,
                       page: Int,
                       pageSize: Int,
                       idList: List[Long],
                       fallback: Boolean) = {
      val result = query match {
        case Some(q) =>
          conceptSearchService.matchingQuery(
            query = q,
            withIdIn = idList,
            searchLanguage = language,
            page = page,
            pageSize = pageSize,
            sort = sort.getOrElse(Sort.ByRelevanceDesc),
            fallback = fallback
          )

        case None =>
          conceptSearchService.all(
            withIdIn = idList,
            language = language,
            page = page,
            pageSize = pageSize,
            sort = sort.getOrElse(Sort.ByIdAsc),
            fallback = fallback
          )
      }

      result match {
        case Success(searchResult) =>
          val responseHeader = searchResult.scrollId.map(i => this.scrollId.paramName -> i).toMap
          Ok(searchConverterService.asApiConceptSearchResult(searchResult), headers = responseHeader)
        case Failure(ex) => errorHandler(ex)
      }
    }

    get(
      "/:concept_id",
      operation(
        apiOperation[String]("getConceptById")
          summary "Fetch specified concept"
          description "Shows the concept for the specified id."
          parameters (
            asHeaderParam(correlationId),
            asPathParam(conceptId),
            asQueryParam(fallback)
        )
          responseMessages (response404, response500))
    ) {
      val conceptId = long(this.conceptId.paramName)
      val language = paramOrDefault(this.language.paramName, Language.NoLanguage)
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)

      readService.conceptWithId(conceptId, language, fallback) match {
        case Success(concept) => concept
        case Failure(ex)      => errorHandler(ex)
      }
    }

  }
}
