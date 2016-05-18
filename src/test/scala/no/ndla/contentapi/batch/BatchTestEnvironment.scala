package no.ndla.contentapi.batch

import no.ndla.contentapi.batch.integration.CMDataComponent
import no.ndla.contentapi.batch.service.converters.{ContentBrowserConverter, SimpleTagConverter}
import no.ndla.contentapi.batch.service.{ConverterModules, ConverterServiceComponent, ImportServiceComponent}
import org.scalatest.mock.MockitoSugar

trait BatchTestEnvironment
  extends CMDataComponent
  with ImportServiceComponent
  with ConverterModules
  with ConverterServiceComponent
  with MockitoSugar {

  val cmData = mock[CMData]
  val importService = mock[ImportService]
  val converterService = mock[ConverterService]
  val converterModules = List(SimpleTagConverter, ContentBrowserConverter)
}
