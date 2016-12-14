/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import no.ndla.articleapi.UnitSuite

class SequenceGeneratorTest extends UnitSuite {
  object Generator extends SequenceGenerator

  test("SequenceGenerator produces a sequence of numbers starting at 1") {
    (1 to 10).foreach(currentNumber => {
      Generator.nextNumberInSequence should equal (currentNumber.toString)
    })
  }

}
