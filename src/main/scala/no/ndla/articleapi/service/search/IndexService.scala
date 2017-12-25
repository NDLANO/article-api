/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.search

import java.text.SimpleDateFormat
import java.util.Calendar

import com.typesafe.scalalogging.LazyLogging
import io.searchbox.core.{Bulk, Delete, Index}
import io.searchbox.indices.aliases.{AddAliasMapping, GetAliases, ModifyAliases, RemoveAliasMapping}
import io.searchbox.indices.mapping.PutMapping
import io.searchbox.indices.{CreateIndex, DeleteIndex, IndicesExists}
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.mappings.MappingDefinition
import no.ndla.articleapi.ArticleApiProperties
import no.ndla.articleapi.integration.{Elastic4sClient, ElasticClient}
import no.ndla.articleapi.model.domain.{Content, NdlaSearchException, ReindexResult}
import no.ndla.articleapi.repository.Repository

import scala.util.{Failure, Success, Try}

trait IndexService {
  this: ElasticClient with Elastic4sClient =>

  trait IndexService[D <: Content, T <: AnyRef] extends LazyLogging {
    val documentType: String
    val searchIndex: String
    val repository: Repository[D]

    def getMapping: MappingDefinition
    def createIndexRequest(domainModel: D, indexName: String): Index

    def indexDocument(imported: D): Try[D] = {
      for {
        _ <- getAliasTarget.map {
          case Some(index) => Success(index)
          case None => createIndexWithGeneratedName.map(newIndex => updateAliasTarget(None, newIndex))
        }
        _ <- jestClient.execute(createIndexRequest(imported, searchIndex))
      } yield imported
    }

    def indexDocuments: Try[ReindexResult] = {
      synchronized {
        val start = System.currentTimeMillis()
        createIndexWithGeneratedName.flatMap(indexName => {
          val operations = for {
            numIndexed <- sendToElastic(indexName)
            aliasTarget <- getAliasTarget
            _ <- updateAliasTarget(aliasTarget, indexName)
            _ <- deleteIndex(aliasTarget)
          } yield numIndexed

          operations match {
            case Failure(f) => {
              deleteIndex(Some(indexName))
              Failure(f)
            }
            case Success(totalIndexed) => {
              Success(ReindexResult(totalIndexed, System.currentTimeMillis() - start))
            }
          }
        })
      }
    }

    def sendToElastic(indexName: String): Try[Int] = {
      var numIndexed = 0
      getRanges.map(ranges => {
        ranges.foreach(range => {
          val numberInBulk = indexDocuments(repository.documentsWithIdBetween(range._1, range._2), indexName)
          numberInBulk match {
            case Success(num) => numIndexed += num
            case Failure(f) => return Failure(f)
          }
        })
        numIndexed
      })
    }

    def getRanges:Try[List[(Long, Long)]] = {
      Try {
        val (minId, maxId) = repository.minMaxId
        Seq.range(minId, maxId).grouped(ArticleApiProperties.IndexBulkSize).map(group => (group.head, group.last + 1)).toList
      }
    }

    def indexDocuments(contents: Seq[D], indexName: String): Try[Int] = {
      val bulkBuilder = new Bulk.Builder()
      contents.foreach(content => bulkBuilder.addAction(createIndexRequest(content, indexName)))

      val response = jestClient.execute(bulkBuilder.build())
      response.map(r => {
        logger.info(s"Indexed ${contents.size} documents. No of failed items: ${r.getFailedItems.size()}")
        contents.size
      })
    }

    def deleteDocument(contentId: Long): Try[_] = {
      for {
        _ <- getAliasTarget.map {
          case Some(index) => Success(index)
          case None => createIndexWithGeneratedName.map(newIndex => updateAliasTarget(None, newIndex))
        }
        deleted <- {
          jestClient.execute(
            new Delete.Builder(s"$contentId").index(searchIndex).`type`(documentType).build()
          )
        }
      } yield deleted
    }

    def createIndexWithGeneratedName: Try[String] = createIndexWithName(searchIndex + "_" + getTimestamp)

    def createIndexWithName(indexName: String): Try[String] = {
      if (indexWithNameExists(indexName).getOrElse(false)) {
        Success(indexName)
      } else {
        val response = e4sClient.execute {
          createIndex(indexName)
            .mappings(getMapping)
            .indexSetting("max_result_window", ArticleApiProperties.ElasticSearchIndexMaxResultWindow)
        }

        response match {
          case Success(_) => Success(indexName)
          case Failure(ex) => Failure(ex)
        }
      }
    }

    def getAliasTarget: Try[Option[String]] = {
      val response = e4sClient.execute{
        getAliases(Nil, List(searchIndex))
      }

      response match {
        case Success(results) => Success(results.result.mappings.headOption.map((t) => t._1.name))
        case Failure(ex) => Failure(ex)
      }
    }

    def updateAliasTarget(oldIndexName: Option[String], newIndexName: String): Try[Any] = {
      if (!indexWithNameExists(newIndexName).getOrElse(false)) {
        Failure(new IllegalArgumentException(s"No such index: $newIndexName"))
      } else {
        val addAliasDefinition = new AddAliasMapping.Builder(newIndexName, searchIndex).build()
        val modifyAliasRequest = oldIndexName match {
          case None => new ModifyAliases.Builder(addAliasDefinition).build()
          case Some(oldIndex) =>
            new ModifyAliases.Builder(
              new RemoveAliasMapping.Builder(oldIndex, searchIndex).build()
            ).addAlias(addAliasDefinition).build()
        }

        jestClient.execute(modifyAliasRequest)
      }
    }

    def deleteIndex(optIndexName: Option[String]): Try[_] = {
      optIndexName match {
        case None => Success(optIndexName)
        case Some(indexName) => {
          if (!indexWithNameExists(indexName).getOrElse(false)) {
            Failure(new IllegalArgumentException(s"No such index: $indexName"))
          } else {
            jestClient.execute(new DeleteIndex.Builder(indexName).build())
          }
        }
      }

    }

    def indexWithNameExists(indexName: String): Try[Boolean] = {
      val response = e4sClient.execute {
        indexExists(indexName)
      }

      response match {
        case Success(resp) if resp.status != 404 => Success(true)
        case Success(_) => Success(false)
        case Failure(ex) => Failure(ex)
      }
    }

    def getTimestamp: String = new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance.getTime)

  }
}
