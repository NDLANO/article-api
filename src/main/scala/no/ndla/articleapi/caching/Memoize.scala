/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.caching

import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import no.ndla.articleapi.ArticleApiProperties.ApiClientsCacheAgeInMs

class Memoize[R](maxCacheAgeMs: Long, f: () => R, autoRefreshCache: Boolean) extends (() => R) {
  case class CacheValue(value: R, lastUpdated: Long) {
    def isExpired: Boolean = lastUpdated + maxCacheAgeMs <= System.currentTimeMillis()
  }

  private[this] var cache: Option[CacheValue] = None

  private def renewCache: Unit = {
    cache = Some(CacheValue(f(), System.currentTimeMillis()))
  }

  if (autoRefreshCache) {
    val ex = new ScheduledThreadPoolExecutor(1)
    val task = new Runnable {
      def run() = renewCache
    }
    ex.scheduleAtFixedRate(task, 20, maxCacheAgeMs, TimeUnit.MILLISECONDS)
  }

  def apply(): R = {
    cache match {
      case Some(cachedValue) if autoRefreshCache       => cachedValue.value
      case Some(cachedValue) if !cachedValue.isExpired => cachedValue.value
      case _ =>
        renewCache
        cache.get.value
    }
  }

}

object Memoize {
  def apply[R](f: () => R) = new Memoize(ApiClientsCacheAgeInMs, f, autoRefreshCache = false)
}

object MemoizeAutoRenew {
  def apply[R](f: () => R) = new Memoize(ApiClientsCacheAgeInMs, f, autoRefreshCache = true)
}
