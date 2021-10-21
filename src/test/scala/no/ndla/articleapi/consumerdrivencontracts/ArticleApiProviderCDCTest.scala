/*
 * Part of NDLA article-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.consumerdrivencontracts

import java.io.IOException
import java.net.ServerSocket

import com.itv.scalapact.ScalaPactVerify._
import com.itv.scalapact.shared.PactBrokerAuthorization.BasicAuthenticationCredentials
import com.itv.scalapact.shared.{BrokerPublishData, ProviderStateResult, TaggedConsumer}
import no.ndla.articleapi._
import no.ndla.articleapi.integration.Elastic4sClientFactory
import no.ndla.scalatestsuite.IntegrationSuite
import org.eclipse.jetty.server.Server
import org.joda.time.DateTime
import org.scalatest.Tag
import scalikejdbc._

import scala.concurrent.duration._
import scala.sys.process._
import scala.util.Properties.{envOrElse, envOrNone}
import scala.util.{Failure, Success, Try}

object PactProviderTest extends Tag("PactProviderTest")

class ArticleApiProviderCDCTest
    extends IntegrationSuite(EnableElasticsearchContainer = true, EnablePostgresContainer = true)
    with UnitSuite
    with TestEnvironment {
  override val dataSource = testDataSource.get

  import com.itv.scalapact.circe13._
  import com.itv.scalapact.http4s21._

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
    DBMigrator.migrate(dataSource)
    ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))
    DB autoCommit (implicit session => {
      val schemaName = SQLSyntax.createUnsafely(dataSource.getSchema)
      sql"drop schema if exists $schemaName cascade;"
        .execute()
    })
    DBMigrator.migrate(dataSource)
    ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    println(s"Running CDC tests with component on localhost:$serverPort")
    server = Some(JettyLauncher.startServer(serverPort))
  }

  override def afterAll(): Unit = {
    super.afterAll()
    server.foreach(_.stop())
  }

  private def setupArticles() =
    (1 to 10)
      .map(id => {
        ComponentRegistry.articleRepository
          .updateArticleFromDraftApi(
            TestData.sampleDomainArticle.copy(
              id = Some(id),
              updated = new DateTime(0).toDate,
              created = new DateTime(0).toDate,
              published = new DateTime(0).toDate
            ),
            List(s"1$id")
          )
      })
      .collectFirst { case Failure(ex) => Failure(ex) }
      .getOrElse(Success(true))

  private def getGitVersion =
    for {
      shortCommit <- Try("git rev-parse --short=7 HEAD".!!.trim)
      dirtyness <- Try("git status --porcelain".!!.trim != "").map {
        case true  => "-dirty"
        case false => ""
      }
    } yield s"$shortCommit$dirtyness"

  test("That pacts from broker are working.", PactProviderTest) {
    val isCI = envOrElse("CI", "false").toBoolean
    val isPullRequest = envOrElse("GITHUB_EVENT_NAME", "false") == "pull_request"
    val publishResults = if (isCI && !isPullRequest) {
      getGitVersion.map(version => BrokerPublishData(version, None)).toOption
    } else { None }

    ComponentRegistry.e4sClient = Elastic4sClientFactory.getClient(elasticSearchHost.get)

    val consumersToVerify = List(
      TaggedConsumer("draft-api", List("master")),
      TaggedConsumer("search-api", List("master"))
    )

    val broker = for {
      url <- envOrNone("PACT_BROKER_URL")
      username <- envOrNone("PACT_BROKER_USERNAME")
      password <- envOrNone("PACT_BROKER_PASSWORD")
      broker <- pactBrokerWithTags(url,
                                   "article-api",
                                   publishResults,
                                   consumersToVerify,
                                   Some(BasicAuthenticationCredentials(username, password)))
    } yield broker

    withFrozenTime(new DateTime(0)) {
      broker match {
        case Some(b) =>
          verifyPact
            .withPactSource(b)
            .setupProviderState("given") {
              case "articles" => deleteSchema(); ProviderStateResult(setupArticles().getOrElse(false))
              case "empty"    => deleteSchema(); ProviderStateResult(true)
            }
            .runVerificationAgainst("localhost", serverPort, 10.seconds)
        case None => throw new RuntimeException("Could not get broker settings...")
      }
    }
  }
}
