/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.controller

import no.ndla.articleapi.ArticleApiProperties
import no.ndla.articleapi.auth.{Role, User}
import no.ndla.articleapi.model.api._
import no.ndla.articleapi.model.domain.{ArticleType, Language, Sort}
import no.ndla.articleapi.service.search.ArticleSearchService
import no.ndla.articleapi.service.{ConverterService, ReadService, WriteService}
import no.ndla.articleapi.validation.ContentValidator
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.NotFound
import org.scalatra.swagger._
import org.scalatra.util.NotNothing

import scala.util.{Failure, Success}

trait ArticleControllerV2 {
  this: ReadService with WriteService with ArticleSearchService with ConverterService with Role with User with ContentValidator =>
  val articleControllerV2: ArticleControllerV2

  class ArticleControllerV2(implicit val swagger: Swagger) extends NdlaController with SwaggerSupport {
    protected implicit override val jsonFormats: Formats = DefaultFormats
    protected val applicationDescription = "Services for accessing articles"

    // Additional models used in error responses
    registerModel[ValidationError]()
    registerModel[Error]()

    val converterService = new ConverterService
    val response400 = ResponseMessage(400, "Validation Error", Some("ValidationError"))
    val response403 = ResponseMessage(403, "Access Denied", Some("Error"))
    val response404 = ResponseMessage(404, "Not found", Some("Error"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    private val correlationId = Param("X-Correlation-ID","User supplied correlation-id. May be omitted.")
    private val query = Param("query","Return only articles with content matching the specified query.")
    private val language = Param("language", "The ISO 639-1 language code describing language.")
    private val license = Param("license","Return only results with provided license.")
    private val sort = Param("sort",
      """The sorting used on results.
             The following are supported: relevance, -relevance, title, -title, lastUpdated, -lastUpdated, id, -id.
             Default is by -relevance (desc) when query is set, and title (asc) when query is empty.""".stripMargin)
    private val pageNo = Param("page","The page number of the search hits to display.")
    private val pageSize = Param("page-size","The number of search hits to display for each page.")
    private val articleId = Param("article_id","Id of the article that is to be fecthed")
    private val size = Param("size", "Limit the number of results to this many elements")
    private val articleTypes = Param("articleTypes", "Return only articles of specific type(s). To provide multiple types, separate by comma (,).")
    private val articleIds = Param("ids","Return only articles that have one of the provided ids. To provide multiple ids, separate by comma (,).")
    private val deprecatedNodeId = Param("deprecated_node_id", "Id of deprecated NDLA node")
    private val fallback = Param("fallback", "Fallback to existing language if language is specified.")

    private def asQueryParam[T: Manifest: NotNothing](param: Param) = queryParam[T](param.paramName).description(param.description)
    private def asHeaderParam[T: Manifest: NotNothing](param: Param) = headerParam[T](param.paramName).description(param.description)
    private def asPathParam[T: Manifest: NotNothing](param: Param) = pathParam[T](param.paramName).description(param.description)


    val getTags =
      (apiOperation[ArticleTag]("getTags")
        summary "Fetch tags used in articles"
        notes "Retrieves a list of all previously used tags in articles"
        parameters(
          asHeaderParam[Option[String]](correlationId),
          asQueryParam[Option[Int]](size),
          asQueryParam[Option[String]](language)
        )
        responseMessages response500
        authorizations "oauth2")

    get("/tags/", operation(getTags)) {
      val defaultSize = 20
      val language = paramOrDefault(this.language.paramName, Language.AllLanguages)
      val size = intOrDefault(this.size.paramName, defaultSize) match {
        case tooSmall if tooSmall < 1 => defaultSize
        case x => x
      }
      val tags = readService.getNMostUsedTags(size, language)
      if (tags.isEmpty) {
        NotFound(body = Error(Error.NOT_FOUND, s"No tags with language $language was found"))
      } else {
        tags
      }
    }

    private def search(query: Option[String], sort: Option[Sort.Value], language: String, license: Option[String], page: Int, pageSize: Int, idList: List[Long], articleTypesFilter: Seq[String]) = {
      query match {
        case Some(q) => articleSearchService.matchingQuery(
          query = q,
          withIdIn = idList,
          searchLanguage = language,
          license = license,
          page = page,
          pageSize = if (idList.isEmpty) pageSize else idList.size,
          sort = sort.getOrElse(Sort.ByRelevanceDesc),
          if (articleTypesFilter.isEmpty) ArticleType.all else articleTypesFilter
        )

        case None => articleSearchService.all(
          withIdIn = idList,
          language = language,
          license = license,
          page = page,
          pageSize = if (idList.isEmpty) pageSize else idList.size,
          sort = sort.getOrElse(Sort.ByTitleAsc),
          if (articleTypesFilter.isEmpty) ArticleType.all else articleTypesFilter
        )
      }
    }

    val getAllArticles =
      (apiOperation[List[SearchResultV2]]("getAllArticles")
        summary "Find articles"
        notes "Shows all articles. You can search it too."
        parameters(
        asHeaderParam[Option[String]](correlationId),
        asQueryParam[Option[String]](articleTypes),
        asQueryParam[Option[String]](query),
        asQueryParam[Option[String]](articleIds),
        asQueryParam[Option[String]](language),
        asQueryParam[Option[String]](license),
        asQueryParam[Option[Int]](pageNo),
        asQueryParam[Option[Int]](pageSize),
        asQueryParam[Option[String]](sort)
      )
        authorizations "oauth2"
        responseMessages(response500))

    get("/", operation(getAllArticles)) {
      val query = paramOrNone(this.query.paramName)
      val sort = Sort.valueOf(paramOrDefault(this.sort.paramName, ""))
      val language = paramOrDefault(this.language.paramName, Language.AllLanguages)
      val license = paramOrNone(this.license.paramName)
      val pageSize = intOrDefault(this.pageSize.paramName, ArticleApiProperties.DefaultPageSize)
      val page = intOrDefault(this.pageNo.paramName, 1)
      val idList = paramAsListOfLong(this.articleIds.paramName)
      val articleTypesFilter = paramAsListOfString(this.articleTypes.paramName)

      search(query, sort, language, license, page, pageSize, idList, articleTypesFilter)
    }

    val getAllArticlesPost =
      (apiOperation[List[SearchResultV2]]("getAllArticlesPost")
        summary "Find articles"
        notes "Shows all articles. You can search it too."
        parameters(
          asHeaderParam[Option[String]](correlationId),
          bodyParam[ArticleSearchParams]
        )
        authorizations "oauth2"
        responseMessages(response400, response500))

    post("/search/", operation(getAllArticlesPost)) {
      val searchParams = extract[ArticleSearchParams](request.body)

      val query = searchParams.query
      val sort = Sort.valueOf(searchParams.sort.getOrElse(""))
      val language = searchParams.language.getOrElse(Language.AllLanguages)
      val license = searchParams.license
      val pageSize = searchParams.pageSize.getOrElse(ArticleApiProperties.DefaultPageSize)
      val page = searchParams.page.getOrElse(1)
      val idList = searchParams.idList
      val articleTypesFilter = searchParams.articleTypes

      search(query, sort, language, license, page, pageSize, idList, articleTypesFilter)
    }

    val getArticleById =
      (apiOperation[List[ArticleV2]]("getArticleById")
        summary "Fetch specified article"
        notes "Shows the article for the specified id."
        parameters(
        asHeaderParam[Option[String]](correlationId),
        asPathParam[Long](articleId),
        asQueryParam[Option[String]](language),
        asQueryParam[Option[Boolean]](fallback)
      )
        authorizations "oauth2"
        responseMessages(response404, response500))

    get("/:article_id", operation(getArticleById)) {
      val articleId = long(this.articleId.paramName)
      val language = paramOrDefault(this.language.paramName, Language.AllLanguages)
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)

      readService.withIdV2(articleId, language, fallback) match {
        case Success(article) => article
        case Failure(ex) => errorHandler(ex)
      }
    }

    val getInternalIdByExternalId =
      (apiOperation[ArticleIdV2]("getInternalIdByExternalId")
        summary "Get id of article corresponding to specified deprecated node id"
        notes "Get internal id of article for a specified ndla_node_id"
        parameters(
        asHeaderParam[Option[String]](correlationId),
        asPathParam[Long](deprecatedNodeId)
      )
        authorizations "oauth2"
        responseMessages(response404, response500))

    get("/external_id/:deprecated_node_id", operation(getInternalIdByExternalId)) {
      val externalId = long(this.deprecatedNodeId.paramName)
      readService.getInternalIdByExternalId(externalId) match {
        case Some(id) => id
        case None => NotFound(body = Error(Error.NOT_FOUND, s"No article with id $externalId"))
      }
    }
  }
}
