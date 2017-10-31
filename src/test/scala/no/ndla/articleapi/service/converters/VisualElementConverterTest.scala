package no.ndla.articleapi.service.converters

import no.ndla.articleapi.ArticleApiProperties.resourceHtmlEmbedTag
import no.ndla.articleapi.model.domain.ImportStatus
import no.ndla.articleapi.{TestData, TestEnvironment, UnitSuite}
import org.mockito.Mockito._

import scala.util.{Failure, Success}

class VisualElementConverterTest extends UnitSuite with TestEnvironment {
  val nodeId = "1234"
  val sampleArticle = TestData.sampleContent.copy(visualElement=Some(nodeId))

  test("visual element of type image should be converted to embed tag") {
    val expectedResult = s"""<$resourceHtmlEmbedTag data-align="" data-alt="" data-caption="" data-resource="image" data-resource_id="1" data-size="" />"""

    when(extractService.getNodeType(nodeId)).thenReturn(Some("image"))
    when(imageApiClient.importImage(nodeId)).thenReturn(Some(TestData.sampleImageMetaInformation))
    val Success((res, _)) = VisualElementConverter.convert(sampleArticle, ImportStatus.empty)

    res.visualElement should equal (Some(expectedResult))
  }

  test("Visual element of type image that cannot be found should return a Failure") {
    when(extractService.getNodeType(nodeId)).thenReturn(Some("image"))
    when(imageApiClient.importImage(nodeId)).thenReturn(None)

    VisualElementConverter.convert(sampleArticle, ImportStatus.empty).isFailure should be (true)
  }

  test("visual element of type audio should be converted to embed tag") {
    val expectedResult = s"""<$resourceHtmlEmbedTag data-resource="audio" data-resource_id="1" />"""

    when(extractService.getNodeType(nodeId)).thenReturn(Some("audio"))
    when(audioApiClient.getOrImportAudio(nodeId)).thenReturn(Success(1: Long))
    val Success((res, _)) = VisualElementConverter.convert(sampleArticle, ImportStatus.empty)

    res.visualElement should equal (Some(expectedResult))
  }

  test("Visual element of type audio that cannot be found should return a failure") {
    when(extractService.getNodeType(nodeId)).thenReturn(Some("audio"))
    when(audioApiClient.getOrImportAudio(nodeId)).thenReturn(Failure(new RuntimeException()))

    VisualElementConverter.convert(sampleArticle, ImportStatus.empty).isFailure should be (true)
  }

  test("visual element of type video should be converted to embed tag") {
    val expectedResult = s"""<$resourceHtmlEmbedTag data-account="some-account-id" data-caption="" data-player="some-player-id" data-resource="brightcove" data-videoid="ref:1234" />"""

    when(extractService.getNodeType(nodeId)).thenReturn(Some("video"))
    val Success((res, _)) = VisualElementConverter.convert(sampleArticle, ImportStatus.empty)
    res.visualElement should equal (Some(expectedResult))
    res.requiredLibraries.size should be (1)
  }

  test("visual element of type h5p should be converted to embed tag") {
    val expectedResult = s"""<$resourceHtmlEmbedTag data-resource="h5p" data-url="//ndla.no/h5p/embed/1234" />"""

    when(extractService.getNodeType(nodeId)).thenReturn(Some("h5p_content"))
    val Success((res, _)) = VisualElementConverter.convert(sampleArticle, ImportStatus.empty)
    res.visualElement should equal (Some(expectedResult))
    res.requiredLibraries.size should be (1)
  }

  test("An empty visual element should return Success without any content modifications") {
    val Success((cont, importStatus)) = VisualElementConverter.convert(sampleArticle.copy(visualElement=None), ImportStatus.empty)
    cont should equal (sampleArticle.copy(visualElement=None))
    importStatus should equal (ImportStatus.empty)
  }

}
