/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.controller

import java.util.Date

import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.{TestData, TestEnvironment, UnitSuite}
import org.json4s.native.Serialization._
import org.scalatra.test.scalatest.ScalatraFunSuite
import org.mockito.Mockito._

import scala.util.{Failure, Try}
import no.ndla.articleapi.TestData._
import org.mockito.Matchers.anyLong

class InternControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {
  implicit val formats = org.json4s.DefaultFormats

  val author = Author("forfatter", "Henrik")
  val sampleNode = NodeToConvert(List(sampleTitle), List(sampleContent), "by-sa", Seq(author), List(ArticleTag(List("tag"), Some("en"))), Seq(visualElement), "fagstoff", new Date(0), new Date(1), ArticleType.Standard)
  val sampleNode2 = sampleNode.copy(contents=List(sampleTranslationContent))
  lazy val controller = new InternController
  addServlet(controller, "/*")

  test("That POST /import/:node_id returns 500 if the main node is not found") {

    when(extractService.getNodeData(nodeId2)).thenReturn(sampleNode2)

    post(s"/import/$nodeId2") {
      status should equal(500)
    }
  }

  test("That POST /import/:node_id returns a json status-object on success") {
    val newNodeid: Long = 4444
    when(extractConvertStoreContent.processNode(nodeId)).thenReturn(Try((newNodeid, ImportStatus(Seq(), Seq()))))

    post(s"/import/$nodeId") {
      status should equal(200)
      val convertedBody = read[ImportStatus](body)
      convertedBody should equal (ImportStatus(s"Successfully imported node $nodeId: $newNodeid", Seq()))
    }
  }

  test("That POST /import/:node_id status code is 500 with a message if processNode fails") {
    when(extractConvertStoreContent.processNode(nodeId)).thenReturn(Failure(new RuntimeException("processNode failed")))

    post(s"/import/$nodeId") {
      status should equal(500)
    }
  }

  test("Test GET csv repport of illegal use of h tags in li elements") {

    var test:Seq[ArticleIds] = Nil
    when(articleRepository.getAllIds()).thenReturn(Seq(ArticleIds(1, None)))
    when(readService.withId(1L)).thenReturn(Some(TestData.apiArticleWithHtmlFault)) //thenReturn(Some(TestData.newArticle))
      get(s"/reports/headerElementsInLists"){
        status should equal(200)
        //Very end of line sensitive, do not adjust code with your IDE!
        body should equal("""artikkel id;feil funnet
1;"html element <h4>Det er ikke lov å gjøre dette.</h4> er ikke lov inni: [<li><h4>Det er ikke lov å gjøre dette.</h4></li>]"
1;"html element <h3>Det er ikke lov å gjøre dette.</h3> er ikke lov inni: [<li><h3>Det er ikke lov å gjøre dette.</h3></li>]"
1;"html element <h2>Det er ikke lov å gjøre dette.</h2> er ikke lov inni: [<li><h2>Det er ikke lov å gjøre dette.</h2></li>]"
1;"html element <h1>Det er ikke lov å gjøre dette.</h1> er ikke lov inni: [<li><h1>Det er ikke lov å gjøre dette.</h1> Tekst utenfor.</li>]"""")
      }
  }

}
