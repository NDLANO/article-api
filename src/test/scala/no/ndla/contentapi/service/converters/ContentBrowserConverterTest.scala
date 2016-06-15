package no.ndla.contentapi.service.converters

import no.ndla.contentapi.TestEnvironment
import no.ndla.contentapi.integration.AudioMeta
import no.ndla.contentapi.model.{Copyright, License}
import no.ndla.contentapi.service.{Image, ImageMetaInformation, ImageVariants}
import no.ndla.contentapi.UnitSuite
import no.ndla.contentapi.ContentApiProperties.amazonUrlPrefix
import org.jsoup.Jsoup
import org.mockito.Mockito._

class ContentBrowserConverterTest extends UnitSuite with TestEnvironment {
  val nodeId = "1234"
  val sampleAltString = "Fotografi"
  val sampleContentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$sampleAltString==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"

  test("That content-browser strings are replaced") {
    val initialContent = s"<article><p>$sampleContentString</p></article>"
    val expectedResult = s"<article> <p>{Unsupported content: ${nodeId}}</p></article>"

    when(extractService.getNodeType(nodeId)).thenReturn(Some(""))
    val (element, requiredLibraries, errors)  = contentBrowserConverter.convert(Jsoup.parseBodyFragment(initialContent).body().child(0))

    element.outerHtml().replace("\n", "") should equal (expectedResult)
    requiredLibraries.length should equal (0)
  }

  test("That content-browser strings of type h5p_content are converted correctly") {
    val initialContent = s"<article>$sampleContentString</article>"
    val expectedResult = s"""<article> <embed data-oembed="http://ndla.no/h5p/embed/${nodeId}" /></article>"""

    when(extractService.getNodeType(nodeId)).thenReturn(Some("h5p_content"))
    val (element, requiredLibraries, errors) = contentBrowserConverter.convert(Jsoup.parseBodyFragment(initialContent).body().child(0))

    element.outerHtml().replace("\n", "") should equal (expectedResult)
    requiredLibraries.length should equal (1)
  }

  test("That Content-browser strings of type image are converted into HTML img tags") {
    val imageUrl = "full.jpeg"
    val initialContent = s"<article><p>$sampleContentString</p></article>"
    val expectedResult = s"""<article> <p><img src="/images/${imageUrl}" alt="${sampleAltString}" /></p></article>"""
    val imageMeta = Some(ImageMetaInformation("1", List(), List(), ImageVariants(Some(Image("small.jpeg", 128, "")), Some(Image(imageUrl, 256, ""))), Copyright(License("", "", Some("")), "", List()), List()))

    when(extractService.getNodeType(nodeId)).thenReturn((Some("image")))
    when(imageApiService.getMetaByExternId(nodeId)).thenReturn(imageMeta)
    val (element, requiredLibraries, errors) = contentBrowserConverter.convert(Jsoup.parseBodyFragment(initialContent).body().child(0))

    element.outerHtml().replace("\n", "") should equal (expectedResult)
    requiredLibraries.length should equal (0)
  }

  test("That Content-browser strings of type audio are extractedand uploaded to an S3 bucket") {
    val initialContent = s"<article>$sampleContentString</article>"
    val audio = AudioMeta(nodeId, "title", "1:23", "mp3", "audio/mpeg", "1024", "sample_audio.mp3", "audio/sample_audio.mp3")
    val destinationPath = s"$nodeId/${audio.filename}"

    when(extractService.getNodeType(nodeId)).thenReturn(Some("audio"))
    when(extractService.getAudioMeta(nodeId)).thenReturn(Some(audio))

    contentBrowserConverter.convert(Jsoup.parseBodyFragment(initialContent).body().child(0))
    verify(storageService, times(1)).uploadAudiofromUrl(nodeId, audio)
  }

  test("That Content-browser strings of type audio are converted into HTML") {
    val initialContent = s"<article>$sampleContentString</article>"
    val filename = "sample_audio.mp3"
    val audio = AudioMeta(nodeId, "title", "1:23", "mp3", "audio/mpeg", "1024", filename, s"audio/$filename")
    val destinationPath = s"$nodeId/${audio.filename}"
    val expectedResult = s"""<article> <figure>   <figcaption>   ${audio.title}  </figcaption>
                             |   <audio src="$amazonUrlPrefix/$destinationPath" preload="auto" controls="">
                             |    Your browser does not support the    <code>video</code> element.
                             |   </audio>
                             |  </figure> </article>""".stripMargin.replace("\n", "")

    when(extractService.getNodeType(nodeId)).thenReturn(Some("audio"))
    when(extractService.getAudioMeta(nodeId)).thenReturn(Some(audio))
    when(storageService.uploadAudiofromUrl(nodeId, audio)).thenReturn(s"$nodeId/$filename")

    val (element, requiredLibs, errors) = contentBrowserConverter.convert(Jsoup.parseBodyFragment(initialContent).body().child(0))
    element.outerHtml().replace("\n", "") should equal(expectedResult)
  }
}
