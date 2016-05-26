package no.ndla.contentapi.batch

import no.ndla.contentapi.batch.integration.CMDataComponent
import no.ndla.contentapi.batch.service.converters.{ContentBrowserConverter, SimpleTagConverter}
import no.ndla.contentapi.batch.service.{ConverterModules, ConverterServiceComponent, ExtractServiceComponent}
import org.scalatest.mock.MockitoSugar

trait BatchTestEnvironment
  extends CMDataComponent
  with ExtractServiceComponent
  with ConverterModules
  with ConverterServiceComponent
  with MockitoSugar
  with ContentBrowserConverter {

  val cmData = mock[CMData]
  val extractService = mock[ExtractService]
  val converterService = mock[ConverterService]
  val contentBrowserConverter = new ContentBrowserConverter
  val converterModules = List(SimpleTagConverter, contentBrowserConverter)
}
