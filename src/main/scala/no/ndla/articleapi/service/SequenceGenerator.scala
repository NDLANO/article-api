/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

trait SequenceGenerator {


  private var currentNumber = 0
  def nextNumberInSequence = {
    currentNumber += 1
    currentNumber.toString
  }
}
