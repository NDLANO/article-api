package no.ndla.contentapi.batch

import no.ndla.contentapi.batch.service.{ConverterModules, ConverterServiceComponent, ExtractServiceComponent}
import no.ndla.contentapi.batch.integration.CMDataComponent
import no.ndla.contentapi.batch.service.converters.{ContentBrowserConverter, SimpleTagConverter}
import no.ndla.contentapi.integration.AmazonIntegration

object BatchComponentRegistry
  extends ExtractServiceComponent
    with ConverterModules
    with ConverterServiceComponent
    with CMDataComponent {

  lazy val CMHost = scala.util.Properties.envOrNone("CM_HOST")
  lazy val CMPort = scala.util.Properties.envOrNone("CM_PORT")
  lazy val CMDatabase = scala.util.Properties.envOrNone("CM_DATABASE")
  lazy val CMUser = scala.util.Properties.envOrNone("CM_USER")
  lazy val CMPassword = scala.util.Properties.envOrNone("CM_PASSWORD")
  lazy val contentData = AmazonIntegration.getContentData()

  lazy val cmData = new CMData(CMHost, CMPort, CMDatabase, CMUser, CMPassword)
  lazy val extractorService = new ExtractService
  lazy val converterService = new ConverterService

  lazy val converterModules = List(ContentBrowserConverter, SimpleTagConverter)
}
