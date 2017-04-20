/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.controller

import no.ndla.articleapi.integration.ConverterModule.stringToJsoupDocument
import no.ndla.articleapi.model.domain.{HtmlFaultRapport, ImportStatus}
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.service._
import no.ndla.articleapi.service.search.SearchIndexService
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.{InternalServerError, Ok}

import scala.collection.JavaConversions._
import scala.collection.immutable
import scala.util.{Failure, Success}

trait InternController {
  this: ReadService with ExtractService with ConverterService with ArticleRepository with ArticleContentInformation
    with ExtractConvertStoreContent with SearchIndexService =>
  val internController: InternController

  class InternController extends NdlaController {

    protected implicit override val jsonFormats: Formats = DefaultFormats

    post("/index") {
      searchIndexService.indexDocuments match {
        case Success(reindexResult) => {
          val result = s"Completed indexing of ${reindexResult.totalIndexed} documents in ${reindexResult.millisUsed} ms."
          logger.info(result)
          Ok(result)
        }
        case Failure(f) => {
          logger.warn(f.getMessage, f)
          InternalServerError(f.getMessage)
        }
      }
    }

    post("/import/:external_id") {
      val externalId = params("external_id")

      extractConvertStoreContent.processNode(externalId) match {
        case Success((newId, status)) => ImportStatus(status.messages :+ s"Successfully imported node $externalId: $newId", status.visitedNodes)
        case Failure(exc) => errorHandler(exc)
      }
    }

    get("/ids") {
      articleRepository.getAllIds
    }

    get("/tagsinuse") {
      ArticleContentInformation.getHtmlTagsMap
    }

    get("/embedurls/:external_subject_id") {
      ArticleContentInformation.getExternalEmbedResources(params("external_subject_id"))
    }

    get("/reports/headerElementsInLists") {
      contentType = "text/csv"
      logger.info("Start searching for header elements in Lists in all articles")
      val start = System.currentTimeMillis()
      var errorMessages: List[HtmlFaultRapport] = immutable.List()
      articleRepository.getAllIds.map(m => {
        val article = readService.withId(m.articleId)
          article match {
          case Some(art) => {
            art.content.map(c => {

              val listElements = stringToJsoupDocument(c.content).select("li")
              val regex = """<h[1-4]>""".r

              listElements.map(li => {
                val allIn = regex.findAllIn(li.toString).toList

                allIn.map(m => {
                  val error = s"html element $m er ikke lov inni <li> elementer, se i: [$li]"
                  errorMessages = HtmlFaultRapport(art.id, error) :: errorMessages
                })
              })
            })

          }
          case None => logger.warn(s"Did not find article given id ${m.articleId} gotten from articleRepository.getAllIds, should be investigated if not due to race condition")
        }
      })
      val stop = System.currentTimeMillis()
      logger.info(s"Done searching for header elements in Lists time taken ${stop - start} ms. Found ${errorMessages.size} faults.")
      //Change the list to CSV format with header row.
      val em = (s"""artikkel id;feil funnet""" :: errorMessages.map(e => s"""${e.articleId};"${e.faultMessage}"""")).mkString("\n")
      Ok(em)
    }
  }

}
