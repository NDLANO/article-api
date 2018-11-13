/*
 * Part of NDLA article-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi

/*
import java.io.IOException
import java.net.ServerSocket

import com.itv.scalapact.ScalaPactVerify._
import com.itv.scalapact.shared.ProviderStateResult
import no.ndla.articleapi.{IntegrationSuite, JettyLauncher, TestData, TestEnvironment}
import org.eclipse.jetty.server.Server
import org.mockito.Mockito._

class VerifyConsumerContractsTest extends IntegrationSuite with TestEnvironment {

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
    println(s"Running CDC tests with component on localhost:$serverPort")
    server = Some(JettyLauncher.startServer(serverPort))

    // Mocking some state for the tests to use
    when(articleRepository.withId(1)).thenReturn(TestData.sampleDomainArticle)
    when(conceptRepository.withId(1)).thenReturn(TestData.sampleConcept)
  }

  override def afterAll(): Unit = server.foreach(_.stop())

  test("That pacts from broker are working.") {
    verifyPact
      .withPactSource(pactBroker("http://pact-broker.ndla-local", "article-api", List("draft-api"), None))
      .setupProviderState("given") { _ =>
        ProviderStateResult(true)
      }
      .runStrictVerificationAgainst("localhost", serverPort)
  }

}*/
