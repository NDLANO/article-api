/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.caching

import no.ndla.articleapi.ArticleApiProperties.ApiClientsCacheAgeInMs

class Memoize1[T, R](maxCacheAgeMs: Long, f: T => R) extends (T => R) {
  case class CacheValue(value: R, lastUpdated: Long) {
    def isExpired: Boolean = lastUpdated + maxCacheAgeMs <= System.currentTimeMillis()
  }

  private[this] val cache = scala.collection.mutable.Map.empty[T, CacheValue]

  def apply(v1: T): R = {
    cache.get(v1) match {
      case Some(cachedValue) if !cachedValue.isExpired => cachedValue.value
      case _ => {
        cache.put(v1, CacheValue(f(v1), System.currentTimeMillis()))
        cache(v1).value
      }
    }
  }

}

object Memoize1 {
  def apply[T, R](f: T => R) = new Memoize1[T, R](ApiClientsCacheAgeInMs, f)
}

class Memoize[R](maxCacheAgeMs: Long, f: () => R) extends(() => R) {
  private[this] val cache = new Memoize1[Option[String], R](maxCacheAgeMs, x => f())
  def apply(): R = cache(None)
}

object Memoize {
  def apply[R](f: () => R) = new Memoize(ApiClientsCacheAgeInMs, f)
}
