/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.caching

import no.ndla.articleapi.ArticleApiProperties.ApiClientsCacheAgeInMs

class Memoize1[T, R](f: T => R, maxCacheAgeMs: Long) extends (T => R) {
  case class CacheValue(value: R, lastUpdated: Long) {
    def isExpired: Boolean = lastUpdated + maxCacheAgeMs <= System.currentTimeMillis()
  }

  private[this] val cache = scala.collection.mutable.Map.empty[T, CacheValue]

  def apply(v1: T): R = {
    cache.get(v1) match {
      case Some(cachedValue) if !cachedValue.isExpired => cachedValue.value
      case _ => {
        cache.put(v1, CacheValue(f(v1), System.currentTimeMillis()))
        cache.get(v1).get.value
      }
    }
  }

}

object Memoize1 {
  def apply[T, R](f: T => R) = new Memoize1[T, R](f, ApiClientsCacheAgeInMs)
}

class Memoize0