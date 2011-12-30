package com.github.scala.android.crud.action

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.Spec

/** A behavior specification for [[com.github.scala.android.crud.action.ContextVars]].
  * @author Eric Pabst (epabst@gmail.com)
  */

@RunWith(classOf[JUnitRunner])
class ContextVarsSpec extends Spec with MustMatchers {
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
