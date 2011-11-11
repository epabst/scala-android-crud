package com.github.scala.android.crud.common

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.Spec

/**
 * A behavior specification for [[com.github.scala.android.crud.common.Common]].
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 4/5/11
 * Time: 7:59 PM
 */

@RunWith(classOf[JUnitRunner])
class CommonSpec extends Spec with MustMatchers {
  describe("tryToEvaluate") {
    it("must evaluate and return the parameter") {
      Common.tryToEvaluate("hello" + " world") must be (Some("hello world"))
    }

    it("must return None if an exception occurs") {
      Common.tryToEvaluate(error("intentional")) must be (None)
    }
  }
}