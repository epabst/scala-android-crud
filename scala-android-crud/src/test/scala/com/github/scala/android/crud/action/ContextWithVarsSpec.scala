package com.github.scala.android.crud.action

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.Spec

/**
 * A behavior specification for [[com.github.scala.android.crud.action.ContextWithVars]].
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 4/5/11
 * Time: 7:59 PM
 */

@RunWith(classOf[JUnitRunner])
class ContextWithVarsSpec extends Spec with MustMatchers {

  describe("ContextVar") {
    it("must retain its value for the same Context") {
      object myVar extends ContextVar[String]
      val context = new ContextVars {}
      val context2 = new ContextVars {}
      myVar.get(context) must be (None)
      myVar.set(context, "hello")
      myVar.get(context) must be (Some("hello"))
      myVar.get(context2) must be (None)
      myVar.get(context) must be (Some("hello"))
    }

    it("clear must clear the value for the same Context") {
      object myVar extends ContextVar[String]
      val myVar2 = new ContextVar[String]
      val context = new ContextVars {}
      val context2 = new ContextVars {}
      myVar.set(context, "hello")
      myVar2.set(context, "howdy")

      myVar.clear(context2) must be (None)
      myVar.get(context) must be (Some("hello"))

      myVar.clear(context) must be (Some("hello"))
      myVar.get(context) must be (None)

      myVar2.clear(context) must be (Some("howdy"))
    }
  }
}
