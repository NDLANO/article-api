/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.search

case class SearchableArticleInformation(id: String,
                                        titles: SearchableTitles,
                                        article: SearchableArticle,
                                        tags: SearchableTags,
                                        license: String,
                                        authors: Seq[String])
