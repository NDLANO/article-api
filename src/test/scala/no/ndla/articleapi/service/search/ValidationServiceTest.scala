/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.search

import no.ndla.articleapi.UnitSuite
import no.ndla.articleapi.service.ValidationService
import no.ndla.articleapi.service.ValidationService.generateTempFile

class ValidationServiceTest extends UnitSuite {
  val schema = """<?xml version="1.0" encoding="UTF-8"?>
                 |<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                 | <xs:element name="article">
                 |   <xs:complexType>
                 |     <xs:sequence>
                 |       <xs:element name="h1" type="xs:string"/>
                 |       <xs:element name="h2" type="xs:string"/>
                 |       <xs:element name="p" minOccurs="0" maxOccurs="unbounded">
                 |         <xs:complexType mixed="true">
                 |           <xs:sequence>
                 |             <xs:element name="strong" type="xs:string" minOccurs="0"/>
                 |             <xs:element name="em" type="xs:string" minOccurs="0"/>
                 |           </xs:sequence>
                 |         </xs:complexType>
                 |       </xs:element>
                 |     </xs:sequence>
                 |   </xs:complexType>
                 | </xs:element>
                 |</xs:schema>""".stripMargin
  val validDocument =
    """<article>
      |<h1>heisann</h1>
      |<h2>heia</h2>
      |</article>
    """.stripMargin

  val invalidDocument =
    """<article>
      |<h1>heisann</h1>
      |</article>
    """.stripMargin

  test("validateXMLSchema returns true on a valid schema") {
    val res = ValidationService.validateHTML(generateTempFile(schema, ".xsd"), generateTempFile(validDocument, ".xml"))
    res should equal (true)
  }

  test("validateXMLSchema returns false on an invalid schema") {
    val res = ValidationService.validateHTML(generateTempFile(schema, ".xsd"), generateTempFile(invalidDocument, ".xml"))
    res should equal (false)
  }

}
