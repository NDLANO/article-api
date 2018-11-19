/*
 * Part of NDLA article-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.CDC

import java.io.IOException
import java.net.ServerSocket

import com.itv.scalapact.ScalaPactVerify._
import com.itv.scalapact.shared.{BrokerPublishData, ProviderStateResult}
import no.ndla.articleapi._
import org.eclipse.jetty.server.Server
import scalikejdbc._

import scala.util.{Success, Try}
import sys.process._

class ArticleApiProviderCDCTest extends IntegrationSuite with TestEnvironment {

  import com.itv.scalapact.circe09._
  import com.itv.scalapact.http4s18._

  def findFreePort: Int = {
    def closeQuietly(socket: ServerSocket): Unit = {
      try {
        socket.close()
      } catch { case _: Throwable => }
    }
    var socket: ServerSocket = null
    try {
      socket = new ServerSocket(0)
      socket.setReuseAddress(true)
      val port = socket.getLocalPort
      closeQuietly(socket)
      return port;
    } catch {
      case e: IOException =>
        logger.trace("Failed to open socket", e);
    } finally {
      if (socket != null) {
        closeQuietly(socket)
      }
    }
    throw new IllegalStateException("Could not find a free TCP/IP port to start embedded Jetty HTTP Server on");
  }

  var server: Option[Server] = None
  val serverPort: Int = findFreePort

  def deleteSchema(): Unit = {
    println("Deleting test schema to prepare for CDC testing...")
    val datasource = testDataSource
    DBMigrator.migrate(datasource)
    ConnectionPool.singleton(new DataSourceConnectionPool(datasource))
    DB autoCommit (implicit session => {
      sql"drop schema if exists articleapitest cascade;"
        .execute()
        .apply()
    })
    DBMigrator.migrate(datasource)
    ConnectionPool.singleton(new DataSourceConnectionPool(datasource))
  }

  override def beforeAll(): Unit = {
    deleteSchema()

    println(s"Running CDC tests with component on localhost:$serverPort")
    server = Some(JettyLauncher.startServer(serverPort))
  }

  override def afterAll(): Unit = server.foreach(_.stop())

  private def setupArticles() =
    (1 to 10)
      .map(_ => ComponentRegistry.articleRepository.allocateArticleId())
      .map(id => {
        ComponentRegistry.articleRepository
          .updateArticleFromDraftApi(TestData.sampleDomainArticle.copy(id = Some(id)), List(s"1$id"))
      })

  private def setupConcepts() =
    (1 to 10)
      .map(_ => ComponentRegistry.conceptRepository.allocateConceptId())
      .map(id => {
        ComponentRegistry.conceptRepository.updateConceptFromDraftApi(TestData.sampleConcept.copy(id = Some(id)))
      })

  test("That pacts from broker are working.") {
    // Get git version to publish validity for
    val versionString = for {
      shortCommit <- Try("git rev-parse --short HEAD".!!.trim)
      dirtyness <- Try("git status --porcelain".!!.trim != "").map {
        case true  => "-dirty"
        case false => ""
      }
    } yield s"$shortCommit$dirtyness"
    val publishResults = versionString.map(version => BrokerPublishData(version, None)).toOption

    verifyPact
      .withPactSource(pactBroker("http://pact-broker.ndla-local", "article-api", List("draft-api"), publishResults))
      .setupProviderState("given") {
        case "articles" =>
          deleteSchema()
          setupArticles()
          ProviderStateResult(true)
        case "concepts" =>
          deleteSchema()
          setupConcepts()
          ProviderStateResult(true)
        case "empty" =>
          deleteSchema()
          ProviderStateResult(true)
      }
      .runStrictVerificationAgainst("localhost", serverPort)
  }
}
