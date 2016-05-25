package no.ndla.contentapi.controller

import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.ComponentRegistry
import org.elasticsearch.indices.IndexMissingException
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.ScalatraServlet
import org.scalatra.json.NativeJsonSupport
import org.scalatra.swagger.{Swagger, SwaggerSupport}
import no.ndla.contentapi.business.{ContentData, ContentSearch}
import no.ndla.contentapi.model.Error._
import no.ndla.contentapi.model.{ContentInformation, ContentSummary, Error}
import no.ndla.contentapi.network.ApplicationUrl
import no.ndla.logging.LoggerContext

import scala.util.Try

class ContentController (implicit val swagger:Swagger) extends ScalatraServlet with NativeJsonSupport with SwaggerSupport with LazyLogging {

  protected implicit override val jsonFormats: Formats = DefaultFormats

  protected val applicationDescription = "API for accessing images from ndla.no."

  val getAllContent =
    (apiOperation[List[ContentSummary]]("getAllContent")
      summary "Show all content"
      notes "Shows all the content. You can search it too."
      parameters (
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
    parameters (
    headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
    headerParam[Option[String]]("app-key").description("Your app-key. May be omitted to access api anonymously, but rate limiting applies on anonymous access."),
    pathParam[String]("content_id").description("Id of the content that is to be returned")
    ))

  before() {
    contentType = formats("json")
    LoggerContext.setCorrelationID(Option(request.getHeader("X-Correlation-ID")))
    ApplicationUrl.set(request)
  }

  after() {
    LoggerContext.clearCorrelationID()
    ApplicationUrl.clear()
  }

  error{
    case e:IndexMissingException =>
      halt(status = 500, body = Error.IndexMissingError)
    case t:Throwable =>
      logger.error(Error.GenericError.toString, t)
      halt(status = 500, body = Error.GenericError)
  }

  val contentRepository: ContentData = ComponentRegistry.contentRepository
  val contentSearch: ContentSearch = ComponentRegistry.elasticContentSearch

  get("/", operation(getAllContent)) {
    val query = params.get("query")
    val language = params.get("language")
    val license = params.get("license")
    val pageSize = params.get("page-size").flatMap(ps => Try(ps.toInt).toOption)
    val page = params.get("page").flatMap(idx => Try(idx.toInt).toOption)
    logger.info("GET / with params query='{}', language={}, license={}, page={}, page-size={}", query, language, license, page, pageSize)

    query match {
      case Some(query) => contentSearch.matchingQuery(
        query = query.toLowerCase().split(" ").map(_.trim),
        language = language,
        license = license,
        page = page,
        pageSize = pageSize)

      case None => contentSearch.all(license = license, page = page, pageSize = pageSize)
    }
  }

  get("/:content_id", operation(getContentById)) {
    val contentId = params("content_id")
    logger.info("GET /{}", contentId)

    if(contentId.forall(_.isDigit)) {
      contentRepository.withId(contentId) match {
        case Some(image) => image
        case None => halt(status = 404, body = Error(NOT_FOUND, s"No content with id $contentId found"))
      }
    } else {
      halt(status = 404, body = Error(NOT_FOUND, s"No content with id $contentId found"))
    }
  }
}
