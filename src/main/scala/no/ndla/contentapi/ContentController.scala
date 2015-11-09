package no.ndla.contentapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.JettyLauncher._
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

  val getContentById =
    (apiOperation[List[ContentMetaInformation]]("getContentById")
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
      ContentMetaSummary("1", "Myklesaken splittet Norge", s"${ApplicationUrl.get()}1", "by-sa"),
      ContentMetaSummary("2", "Hva er utholdenhet", s"${ApplicationUrl.get()}2", "by-sa"),
      ContentMetaSummary("3", "Potenser", s"${ApplicationUrl.get()}3", "by-sa")
    )
  }

  val testdata = Map(
    "1" ->
      ContentMetaInformation("1",
        List(ContentTitle("Myklesaken splittet Norge", Some("nb"))),
        io.Source.fromInputStream(getClass.getResourceAsStream(s"/testdata/1.html")).mkString,
        Copyright(License("by-nc-sa", "Creative Commons Attribution-NonCommercial-ShareAlike 2.0 Generic", Some("https://creativecommons.org/licenses/by-nc-sa/2.0/")), "NTB Tema", List(Author("forfatter", "Ingrid Brubaker"))),
        List(ContentTag("myklesaken", Some("nb")), ContentTag("norge", Some("nb"))), List()),

    "2" -> ContentMetaInformation("2",
      List(ContentTitle("Hva er utholdenhet", Some("nb"))),
      io.Source.fromInputStream(getClass.getResourceAsStream(s"/testdata/2.html")).mkString,
      Copyright(License("by-nc-sa", "Creative Commons Attribution-NonCommercial-ShareAlike 2.0 Generic", Some("https://creativecommons.org/licenses/by-nc-sa/2.0/")), "Ukjent", List(Author("forfatter", "Oddbjørg Vatn Slapgaard"))),
      List(ContentTag("utholdenhet", Some("nb")), ContentTag("aerob", Some("nb"))), List()),

  "3" -> ContentMetaInformation("3",
    List(ContentTitle("Potenser", Some("nb"))),
    io.Source.fromInputStream(getClass.getResourceAsStream(s"/testdata/3.html")).mkString,
    Copyright(License("by-nc-sa", "Creative Commons Attribution-NonCommercial-ShareAlike 2.0 Generic", Some("https://creativecommons.org/licenses/by-nc-sa/2.0/")), "Ukjent", List(Author("forfatter", "Noen"))),
    List(ContentTag("potenser", Some("nb")), ContentTag("matematikk", Some("nb"))), List(RequiredLibrary("text/javascript", "MathJax", "https://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML")))
  )


  get("/:content_id", operation(getContentById)) {
    val contentId = params("content_id")
    testdata get contentId match {
      case Some(x) => x
      case None => halt(status = 404, body = Error(NOT_FOUND, s"No content with id $contentId found"))
    }
  }
}
