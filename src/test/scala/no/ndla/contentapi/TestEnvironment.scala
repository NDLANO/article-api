package no.ndla.contentapi

import javax.sql.DataSource

import com.sksamuel.elastic4s.ElasticClient
import no.ndla.contentapi.integration.{CMDataComponent, DataSourceComponent, ElasticClientComponent}
import no.ndla.contentapi.repository.ContentRepositoryComponent
import no.ndla.contentapi.service.converters.SimpleTagConverter
import no.ndla.contentapi.service._
import no.ndla.contentapi.service.converters.contentbrowser._
import org.scalatest.mock.MockitoSugar


trait TestEnvironment
  extends ElasticClientComponent
  with ElasticContentSearchComponent
  with ElasticContentIndexComponent
  with DataSourceComponent
  with ContentRepositoryComponent
  with MockitoSugar
  with CMDataComponent
  with ExtractServiceComponent
  with ConverterModules
  with ConverterServiceComponent
  with ImageConverterModule
  with LenkeConverterModule
  with H5PConverterModule
  with FagstoffConverterModule
  with ContentBrowserConverter
  with ImageApiServiceComponent
{
  val elasticClient = mock[ElasticClient]
  val elasticContentSearch = mock[ElasticContentSearch]
  val elasticContentIndex = mock[ElasticContentIndex]

  val dataSource = mock[DataSource]
  val contentRepository = new ContentRepository

  val cmData = mock[CMData]
  val extractService = mock[ExtractService]
  val converterService = mock[ConverterService]
  val contentBrowserConverter = new ContentBrowserConverter
  val converterModules = List(SimpleTagConverter, contentBrowserConverter)
  val imageApiService = mock[ImageApiService]
}
