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

  override def beforeAll(): Unit = {
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
    }
    deleteSchema()

    println(s"Running CDC tests with component on localhost:$serverPort")
    server = Some(JettyLauncher.startServer(serverPort))

    // Setting up some state for the tests to use
    val id = ComponentRegistry.articleRepository.allocateArticleId()
    ComponentRegistry.articleRepository
      .updateArticleFromDraftApi(TestData.sampleDomainArticle.copy(id = Some(id)), List("1234"))
  }

  override def afterAll(): Unit = server.foreach(_.stop())

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
      .setupProviderState("given") { _ =>
        ProviderStateResult(true)
      }
      .runStrictVerificationAgainst("localhost", serverPort)
  }
}
