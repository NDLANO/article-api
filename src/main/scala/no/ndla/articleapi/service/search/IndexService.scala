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
import no.ndla.articleapi.ArticleApiProperties
import no.ndla.articleapi.integration.ElasticClient
import no.ndla.articleapi.model.domain.{Content, NdlaSearchException, ReindexResult}
import no.ndla.articleapi.repository.Repository

import scala.util.{Failure, Success, Try}

trait IndexService {
  this: ElasticClient =>

  trait IndexService[D <: Content, T <: AnyRef] extends LazyLogging {
    val documentType: String
    val searchIndex: String
    val repository: Repository[D]

    def getMapping: String
    def createIndexRequest(domainModel: D, indexName: String): Index

    def indexDocument(imported: D): Try[D] = {
      for {
        _ <- getAliasTarget.map {
          case Some(index) => Success(index)
          case None => createIndex.map(newIndex => updateAliasTarget(None, newIndex))
        }
        _ <- jestClient.execute(createIndexRequest(imported, searchIndex))
      } yield imported
    }

    def indexDocuments: Try[ReindexResult] = {
      synchronized {
        val start = System.currentTimeMillis()
        createIndex.flatMap(indexName => {
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
          case None => createIndex.map(newIndex => updateAliasTarget(None, newIndex))
        }
        deleted <- {
          jestClient.execute(
            new Delete.Builder(s"$contentId").index(searchIndex).`type`(documentType).build()
          )
        }
      } yield deleted
    }

    def createIndex: Try[String] = createIndexWithName(searchIndex + "_" + getTimestamp)

    def createIndexWithName(indexName: String): Try[String] = {
      if (indexExists(indexName)) {
        Success(indexName)
      } else {
        val createIndexResponse = jestClient.execute(
          new CreateIndex.Builder(indexName)
            .settings(s"{\"index\" : { \"max_result_window\" : ${ArticleApiProperties.ElasticSearchIndexMaxResultWindow} }")
            .build())
        createIndexResponse.map(_ => createMapping(indexName)).map(_ => indexName)
      }
    }

    def createMapping(indexName: String): Try[String] = {
      val mappingResponse = jestClient.execute(new PutMapping.Builder(indexName, documentType, getMapping).build())
      mappingResponse.map(_ => indexName)
    }

    def getAliasTarget: Try[Option[String]] = {
      val getAliasRequest = new GetAliases.Builder().addIndex(searchIndex).build()
      jestClient.execute(getAliasRequest) match {
        case Success(result) => {
          val aliasIterator = result.getJsonObject.entrySet().iterator()
          aliasIterator.hasNext match {
            case true => Success(Some(aliasIterator.next().getKey))
            case false => Success(None)
          }
        }
        case Failure(_: NdlaSearchException) => Success(None)
        case Failure(t: Throwable) => Failure(t)
      }
    }

    def updateAliasTarget(oldIndexName: Option[String], newIndexName: String): Try[Any] = {
      if (!indexExists(newIndexName)) {
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
          if (!indexExists(indexName)) {
            Failure(new IllegalArgumentException(s"No such index: $indexName"))
          } else {
            jestClient.execute(new DeleteIndex.Builder(indexName).build())
          }
        }
      }

    }

    def indexExists(indexName: String): Boolean = jestClient.execute(new IndicesExists.Builder(indexName).build()).isSuccess

    def getTimestamp: String = new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance.getTime)

  }
}
