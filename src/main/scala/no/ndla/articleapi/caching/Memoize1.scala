/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.caching

import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import no.ndla.articleapi.ArticleApiProperties.ApiClientsCacheAgeInMs

class Memoize1[T, R](maxCacheAgeMs: Long, f: T => R, autoRefreshCache: Boolean) extends (T => R) {
  case class CacheValue(value: R, lastUpdated: Long) {
    def isExpired: Boolean = lastUpdated + maxCacheAgeMs <= System.currentTimeMillis()
  }

  private[this] val cache = scala.collection.mutable.Map.empty[T, CacheValue]

  private def renewCache(v1: T) = {
    cache.put(v1, CacheValue(f(v1), System.currentTimeMillis()))
  }

  if (autoRefreshCache) {
    val ex = new ScheduledThreadPoolExecutor(1)
    val task = new Runnable {
      def run() = {
        cache.keys.map(renewCache)
      }
    }
    ex.scheduleAtFixedRate(task, 20, maxCacheAgeMs, TimeUnit.MILLISECONDS)
  }

  def apply(v1: T): R = {
    cache.get(v1) match {
      case Some(cachedValue) if autoRefreshCache => cachedValue.value
      case Some(cachedValue) if !cachedValue.isExpired => cachedValue.value
      case _ => {
        renewCache(v1)
        cache(v1).value
      }
    }
  }

}

object Memoize1 {
  def apply[T, R](f: T => R) = new Memoize1[T, R](ApiClientsCacheAgeInMs, f, autoRefreshCache = false)
}

class Memoize[R](maxCacheAgeMs: Long, f: () => R, autoRenewCache: Boolean = false) extends(() => R) {
  private[this] val cache = new Memoize1[Option[String], R](maxCacheAgeMs, x => f(), autoRenewCache)
  def apply(): R = cache(None)
}

object Memoize {
  def apply[R](f: () => R) = new Memoize(ApiClientsCacheAgeInMs, f, autoRenewCache = false)
}

object MemoizeAutoRenew {
  def apply[R](f: () => R) = new Memoize(ApiClientsCacheAgeInMs, f, autoRenewCache = true)
}
