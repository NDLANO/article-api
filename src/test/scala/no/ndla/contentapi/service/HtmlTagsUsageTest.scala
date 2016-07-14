package no.ndla.contentapi.service

import no.ndla.contentapi.model._
import no.ndla.contentapi.{TestEnvironment, UnitSuite}
import org.mockito.Mockito._

class HtmlTagsUsageTest extends UnitSuite with TestEnvironment {

  val copyright = Copyright(License("publicdomain", "", None), "", List())
  val content1 = ContentInformation("1", Seq(ContentTitle("test", Some("en"))), Seq(Content("<article><div>test</div></article>", None, Some("en"))), copyright, Seq(), Seq())
  val content2 = ContentInformation("2", Seq(ContentTitle("test", Some("en"))), Seq(Content("<article><div>test</div><p>paragraph</p></article>", None, Some("en"))), copyright, Seq(), Seq())
  val content3 = ContentInformation("3", Seq(ContentTitle("test", Some("en"))), Seq(Content("<article><img></img></article>", None, Some("en"))), copyright, Seq(), Seq())

  test("That getHtmlTagsMap counts html elements correctly") {
    val expectedResult = Map("article" -> List("1", "2", "3"), "div" -> List("1", "2"), "p" -> List("2"), "img" -> List("3"))
    when(contentRepository.all).thenReturn(List(content1, content2, content3))
    HtmlTagsUsage.getHtmlTagsMap should equal (expectedResult)
  }

  test("That getHtmlTagsMap returns an empty map if no content is available") {
    when(contentRepository.all).thenReturn(List())
    HtmlTagsUsage.getHtmlTagsMap should equal (Map())
  }
}
