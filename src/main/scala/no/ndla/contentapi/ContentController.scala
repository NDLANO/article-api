package no.ndla.contentapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.business.ContentData
import no.ndla.contentapi.integration.AmazonIntegration
import no.ndla.contentapi.model._
import no.ndla.contentapi.network.ApplicationUrl
import no.ndla.contentapi.model.Error
import no.ndla.contentapi.model.Error._
import no.ndla.logging.LoggerContext
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.ScalatraServlet
import org.scalatra.json.NativeJsonSupport
import org.scalatra.swagger.{SwaggerSupport, Swagger}

class ContentController (implicit val swagger:Swagger) extends ScalatraServlet with NativeJsonSupport with SwaggerSupport with LazyLogging {

  protected implicit override val jsonFormats: Formats = DefaultFormats

  //Swagger
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
      queryParam[Option[String]]("license").description("Return only content with provided license.")
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

  get("/", operation(getAllContent)) {
    List(
      ContentSummary("1", "Myklesaken splittet Norge", s"${ApplicationUrl.get()}1", "by-sa"),
      ContentSummary("2", "Hva er utholdenhet", s"${ApplicationUrl.get()}2", "by-sa"),
      ContentSummary("3", "Potenser", s"${ApplicationUrl.get()}3", "by-sa"),
      ContentSummary("4", "Bygg fordøyelsessystemet", s"${ApplicationUrl.get()}4", "by-sa")
    )
  }

  val contentData: ContentData = AmazonIntegration.getContentData()

  get("/:content_id", operation(getContentById)) {
    val contentId = params("content_id")
    logger.info("GET /{}", contentId)

    if(contentId.forall(_.isDigit)) {
      contentData.withId(contentId) match {
        case Some(image) => image
        case None => halt(status = 404, body = Error(NOT_FOUND, s"No content with id $contentId found"))
      }
    } else {
      halt(status = 404, body = Error(NOT_FOUND, s"No content with id $contentId found"))
    }
  }
}
