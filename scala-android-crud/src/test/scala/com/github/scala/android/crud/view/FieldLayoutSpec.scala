package com.github.scala.android.crud.view

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import xml.NodeSeq

/** A behavior specification for [[com.github.scala.android.crud.view.FieldLayout]].
  * @author Eric Pabst (epabst@gmail.com)
  */

@RunWith(classOf[JUnitRunner])
class FieldLayoutSpec extends Spec with MustMatchers {

  describe("toDisplayName") {
    import FieldLayout._
    it("must add a space before capital letters") {
      toDisplayName("anIdentifier").filter(_ == ' ').size must be (1)
    }

    it("must uppercase the first character") {
      toDisplayName("anIdentifier") must be ("An Identifier")
    }

    it("must replace '_' with a space and upper case the next letter") {
      toDisplayName("a_cool_identifier") must be ("A Cool Identifier")
    }
  }

  describe("suppressEdit") {
    it("must make the editXml empty but preserve the displayXml") {
      val fieldLayout = FieldLayout.datePickerLayout
      fieldLayout.suppressEdit.editXml must be (NodeSeq.Empty)
      fieldLayout.suppressEdit.displayXml must be (fieldLayout.displayXml)
    }
  }

  describe("suppressDisplay") {
    it("must make the displayXml empty but preserve the editXml") {
      val fieldLayout = FieldLayout.datePickerLayout
      fieldLayout.suppressDisplay.displayXml must be (NodeSeq.Empty)
      fieldLayout.suppressDisplay.editXml must be (fieldLayout.editXml)
    }
  }
}
