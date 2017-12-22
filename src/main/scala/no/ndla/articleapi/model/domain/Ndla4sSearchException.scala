package no.ndla.articleapi.model.domain

import com.sksamuel.elastic4s.http.RequestFailure

case class Ndla4sSearchException(rf: RequestFailure) extends RuntimeException( //TODO: Rename
  s"""
     |index: ${rf.error.index.getOrElse("Error did not contain index")}
     |reason: ${rf.error.reason}
     |body: ${rf.body}
     |shard: ${rf.error.shard.getOrElse("Error did not contain shard")}
     |type: ${rf.error.`type`}
   """.stripMargin
)
