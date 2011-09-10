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

  describe("toId") {
    import FieldLayout._
    it("must strip whitespace") {
      toId(" an Identifier \t ") must be ("anIdentifier")
    }

    it("must lowercase the first character") {
      toId("AnIdentifier") must be ("anIdentifier")
    }
  }
}
