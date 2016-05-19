package no.ndla.contentapi.batch

import no.ndla.contentapi.batch.service.{ConverterModules, ConverterServiceComponent, ImportServiceComponent}
import no.ndla.contentapi.batch.integration.CMDataComponent
import no.ndla.contentapi.batch.service.converters.{ContentBrowserConverter, DivTableConverter, SimpleTagConverter}

object BatchComponentRegistry
  extends ImportServiceComponent
    with ConverterModules
    with ConverterServiceComponent
    with CMDataComponent {

  lazy val CMHost = scala.util.Properties.envOrNone("CM_HOST")
  lazy val CMPort = scala.util.Properties.envOrNone("CM_PORT")
  lazy val CMDatabase = scala.util.Properties.envOrNone("CM_DATABASE")
  lazy val CMUser = scala.util.Properties.envOrNone("CM_USER")
  lazy val CMPassword = scala.util.Properties.envOrNone("CM_PASSWORD")

  lazy val cmData = new CMData(CMHost, CMPort, CMDatabase, CMUser, CMPassword)
  lazy val importService = new ImportService
  lazy val converterService = new ConverterService

  lazy val converterModules = List(DivTableConverter, ContentBrowserConverter, SimpleTagConverter)
}
