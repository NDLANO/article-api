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

class InternControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {
  implicit val formats = org.json4s.DefaultFormats

  val author = Author("forfatter", "Henrik")
  val sampleNode = NodeToConvert(List(sampleTitle), List(sampleContent), "by-sa", Seq(author), List(ArticleTag(List("tag"), "en")), "fagstoff", "fagstoff", new Date(0), new Date(1), ArticleType.Standard)
  val sampleNode2 = sampleNode.copy(contents = List(sampleTranslationContent))
  val authHeaderWithWriteRole = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoieHh4eXl5IiwiaXNzIjoiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCJzdWIiOiJ4eHh5eXlAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNTEwMzA1NzczLCJleHAiOjE1MTAzOTIxNzMsInNjb3BlIjoiYXJ0aWNsZXM6d3JpdGUiLCJndHkiOiJjbGllbnQtY3JlZGVudGlhbHMifQ.kh82qM84FZgoo3odWbHTLWy-N049m7SyQw4gdatDMk43H2nWHA6gjsbJoiBIZ7BcbSfHElEZH0tP94vRy-kjgA3hflhOBbsD73DIxRvnbH1kSXlBnl6ISbgtHnzv1wQ7ShykMAcBsoWQ6J16ixK_p-msW42kcEqK1LanzPy-_qI"
  lazy val controller = new InternController
  addServlet(controller, "/*")

  test("That POST /import/:node_id returns 500 if the main node is not found") {

    when(extractService.getNodeData(nodeId2)).thenReturn(sampleNode2)

    post(s"/import/$nodeId2", headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal(500)
    }
  }

  test("That POST /import/:node_id returns a json status-object on success") {
    val newNodeId: Long = 4444
    val newArticle = TestData.sampleArticleWithByNcSa.copy(id=Some(newNodeId))
    when(extractConvertStoreContent.processNode(nodeId, false)).thenReturn(Try((newArticle, ImportStatus.empty)))

    post(s"/import/$nodeId", params = Map("forceUpdate" -> "false"), headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal(200)
      val convertedBody = read[ImportStatus](body)
      convertedBody should equal(ImportStatus(s"Successfully imported node $nodeId: $newNodeId", Set[String]()))
    }
  }

  test("That POST /import/:node_id status code is 500 with a message if processNode fails") {
    when(extractConvertStoreContent.processNode(nodeId, false)).thenReturn(Failure(new RuntimeException("processNode failed")))

    post(s"/import/$nodeId", params = Map("forceUpdate" -> "false"), headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal(500)
    }
  }

  test("Test GET csv repport of illegal use of h tags in li elements") {

    var test: Seq[ArticleIds] = Nil
    when(articleRepository.getAllIds()).thenReturn(Seq(ArticleIds(1, None)))
    when(articleRepository.withId(1L)).thenReturn(Some(TestData.sampleDomainArticleWithHtmlFault)) //thenReturn(Some(TestData.newArticle))
    get(s"/reports/headerElementsInLists") {
      status should equal(200)
      //Very end of line sensitive, do not adjust code with your IDE!
      body should equal(
        """artikkel id;feil funnet
1;"html element <h4>Det er ikke lov å gjøre dette.</h4> er ikke lov inni: [<li><h4>Det er ikke lov å gjøre dette.</h4></li>]"
1;"html element <h3>Det er ikke lov å gjøre dette.</h3> er ikke lov inni: [<li><h3>Det er ikke lov å gjøre dette.</h3></li>]"
1;"html element <h2>Det er ikke lov å gjøre dette.</h2> er ikke lov inni: [<li><h2>Det er ikke lov å gjøre dette.</h2></li>]"
1;"html element <h1>Det er ikke lov å gjøre dette.</h1> er ikke lov inni: [<li><h1>Det er ikke lov å gjøre dette.</h1> Tekst utenfor.</li>]"""")
    }
  }

}
