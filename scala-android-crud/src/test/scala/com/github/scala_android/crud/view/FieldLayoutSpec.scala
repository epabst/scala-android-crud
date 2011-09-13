package com.github.scala_android.crud.view

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers

/**
 * A behavior specification for {@link FieldLayout}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 9/10/11
 * Time: 10:01 AM
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
}
