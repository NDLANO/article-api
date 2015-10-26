package no.ndla.contentapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.JettyLauncher._
import no.ndla.contentapi.model._
import no.ndla.contentapi.network.ApplicationUrl
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
    (apiOperation[List[ContentMetaSummary]]("getAllContent")
      summary "Show all content"
      notes "Shows all the content. You can search it too."
      parameters (
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      headerParam[Option[String]]("app-key").description("Your app-key. May be omitted to access api anonymously, but rate limiting applies on anonymous access."),
      queryParam[Option[String]]("tags").description("Return only content with submitted tag. Multiple tags may be entered comma separated, and will give results matching either one of them."),
      queryParam[Option[String]]("language").description("The ISO 639-1 language code describing language used in query-params."),
      queryParam[Option[String]]("license").description("Return only content with provided license.")
      ))

  before() {
    contentType = formats("json")
    LoggerContext.setCorrelationID(Option(request.getHeader("X-Correlation-ID")))
    ApplicationUrl.set(request)
  }

  after() {
    LoggerContext.clearCorrelationID
    ApplicationUrl.clear()
  }

  get("/", operation(getAllContent)) {
    List(
      ContentMetaSummary("1", "Myklesaken splittet Norge", s"${ApplicationUrl.get()}1", "by-sa"),
      ContentMetaSummary("2", "HMS Spillet", s"${ApplicationUrl.get()}2", "by-sa")
    )
  }

  get("/:content_id") {
    io.Source.fromInputStream(getClass.getResourceAsStream(s"/testdata/${params("content_id")}.json")).mkString
  }
}
