/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.search

import com.sksamuel.elastic4s.analyzers.StandardAnalyzer

import java.text.SimpleDateFormat
import java.util.Calendar
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.indexes.IndexRequest
import com.sksamuel.elastic4s.mappings.dynamictemplate.DynamicTemplateRequest
import com.sksamuel.elastic4s.mappings.{FieldDefinition, MappingDefinition}
import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.ArticleApiProperties
import no.ndla.articleapi.integration.Elastic4sClient
import no.ndla.articleapi.model.domain.Language.languageAnalyzers
import no.ndla.articleapi.model.domain.{Content, ReindexResult}
import no.ndla.articleapi.repository.Repository

import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}

trait IndexService {
  this: Elastic4sClient =>

  trait IndexService[D <: Content, T <: AnyRef] extends LazyLogging {
    val documentType: String
    val searchIndex: String
    val repository: Repository[D]

    def getMapping: MappingDefinition

    def createIndexRequest(domainModel: D, indexName: String): IndexRequest

    def indexDocument(imported: D): Try[D] = {
      for {
        _ <- getAliasTarget.map {
          case Some(index) => Success(index)
          case None        => createIndexWithGeneratedName.map(newIndex => updateAliasTarget(None, newIndex))
        }
        _ <- e4sClient.execute {
          createIndexRequest(imported, searchIndex)
        }
      } yield imported
    }

    def indexDocuments: Try[ReindexResult] = synchronized {
      val start = System.currentTimeMillis()
      createIndexWithGeneratedName.flatMap(indexName => {
        val operations = for {
          numIndexed <- sendToElastic(indexName)
          aliasTarget <- getAliasTarget
          _ <- updateAliasTarget(aliasTarget, indexName)
          _ <- deleteIndexWithName(aliasTarget)
        } yield numIndexed

        operations match {
          case Failure(f) =>
            deleteIndexWithName(Some(indexName))
            Failure(f)
          case Success(totalIndexed) =>
            Success(ReindexResult(totalIndexed, System.currentTimeMillis() - start))
        }
      })
    }

    def sendToElastic(indexName: String): Try[Int] = {
      var numIndexed = 0
      getRanges.map(ranges => {
        ranges.foreach(range => {
          val numberInBulk = indexDocuments(repository.documentsWithIdBetween(range._1, range._2), indexName)
          numberInBulk match {
            case Success(num) => numIndexed += num
            case Failure(f)   => return Failure(f)
          }
        })
        numIndexed
      })
    }

    def getRanges: Try[List[(Long, Long)]] = {
      Try {
        val (minId, maxId) = repository.minMaxId
        Seq
          .range(minId, maxId + 1)
          .grouped(ArticleApiProperties.IndexBulkSize)
          .map(group => (group.head, group.last))
          .toList
      }
    }

    def indexDocuments(contents: Seq[D], indexName: String): Try[Int] = {
      if (contents.isEmpty) {
        Success(0)
      } else {
        val response = e4sClient.execute {
          bulk(contents.map(content => {
            createIndexRequest(content, indexName)
          }))
        }

        response match {
          case Success(r) =>
            logger.info(s"Indexed ${contents.size} documents. No of failed items: ${r.result.failures.size}")
            Success(contents.size)
          case Failure(ex) => Failure(ex)
        }
      }
    }

    def deleteDocument(contentId: Long): Try[Long] = {
      for {
        _ <- getAliasTarget.map {
          case Some(index) => Success(index)
          case None        => createIndexWithGeneratedName.map(newIndex => updateAliasTarget(None, newIndex))
        }
        _ <- {
          e4sClient.execute {
            delete(s"$contentId").from(searchIndex / documentType)
          }
        }
      } yield contentId
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
          case Success(_)  => Success(indexName)
          case Failure(ex) => Failure(ex)
        }
      }
    }

    def findAllIndexes(indexName: String): Try[Seq[String]] = {
      val response = e4sClient.execute {
        getAliases()
      }

      response match {
        case Success(results) =>
          Success(results.result.mappings.toList.map { case (index, _) => index.name }.filter(_.startsWith(indexName)))
        case Failure(ex) =>
          Failure(ex)
      }
    }

    def getAliasTarget: Try[Option[String]] = {
      val response = e4sClient.execute {
        getAliases(Nil, List(searchIndex))
      }

      response match {
        case Success(results) =>
          Success(results.result.mappings.headOption.map((t) => t._1.name))
        case Failure(ex) => Failure(ex)
      }
    }

    def updateAliasTarget(oldIndexName: Option[String], newIndexName: String): Try[Any] = {
      if (!indexWithNameExists(newIndexName).getOrElse(false)) {
        Failure(new IllegalArgumentException(s"No such index: $newIndexName"))
      } else {
        oldIndexName match {
          case None =>
            e4sClient.execute(addAlias(searchIndex).on(newIndexName))
          case Some(oldIndex) =>
            e4sClient.execute {
              removeAlias(searchIndex).on(oldIndex)
              addAlias(searchIndex).on(newIndexName)
            }
        }
      }
    }

    def deleteIndexWithName(optIndexName: Option[String]): Try[_] = {
      optIndexName match {
        case None => Success(optIndexName)
        case Some(indexName) =>
          if (!indexWithNameExists(indexName).getOrElse(false)) {
            Failure(new IllegalArgumentException(s"No such index: $indexName"))
          } else {
            e4sClient.execute {
              deleteIndex(indexName)
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
        case Success(_)                          => Success(false)
        case Failure(ex)                         => Failure(ex)
      }
    }

    def getTimestamp: String = new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance.getTime)

    /**
      * Returns Sequence of FieldDefinitions for a given field.
      *
      * @param fieldName Name of field in mapping.
      * @param keepRaw   Whether to add a keywordField named raw.
      *                  Usually used for sorting, aggregations or scripts.
      * @return Sequence of FieldDefinitions for a field.
      */
    protected def generateLanguageSupportedFieldList(fieldName: String,
                                                     keepRaw: Boolean = false): Seq[FieldDefinition] = {
      keepRaw match {
        case true =>
          languageAnalyzers.map(
            langAnalyzer =>
              textField(s"$fieldName.${langAnalyzer.languageTag.toString}")
                .fielddata(false)
                .analyzer(langAnalyzer.analyzer)
                .fields(keywordField("raw")))
        case false =>
          languageAnalyzers.map(
            langAnalyzer =>
              textField(s"$fieldName.${langAnalyzer.languageTag.toString}")
                .fielddata(false)
                .analyzer(langAnalyzer.analyzer))
      }
    }

    /**
      * Returns Sequence of DynamicTemplateRequest for a given field.
      *
      * @param fieldName Name of field in mapping.
      * @param keepRaw   Whether to add a keywordField named raw.
      *                  Usually used for sorting, aggregations or scripts.
      * @return Sequence of DynamicTemplateRequest for a field.
      */
    protected def generateLanguageSupportedDynamicTemplates(fieldName: String,
                                                            keepRaw: Boolean = false): Seq[DynamicTemplateRequest] = {
      val fields = new ListBuffer[FieldDefinition]()
      if (keepRaw) {
        fields += keywordField("raw")
      }
      val languageTemplates = languageAnalyzers.map(
        languageAnalyzer => {
          val name = s"$fieldName.${languageAnalyzer.languageTag.toString()}"
          DynamicTemplateRequest(
            name = name,
            mapping = textField(name).analyzer(languageAnalyzer.analyzer).fields(fields.toList),
            matchMappingType = Some("string"),
            pathMatch = Some(name)
          )
        }
      )
      val catchAlltemplate = DynamicTemplateRequest(
        name = fieldName,
        mapping = textField(fieldName).analyzer(StandardAnalyzer).fields(fields.toList),
        matchMappingType = Some("string"),
        pathMatch = Some(s"$fieldName.*")
      )
      languageTemplates ++ Seq(catchAlltemplate)
    }
  }

}
