package com.github.scala.android.crud.action

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.Spec
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar

/** A behavior specification for [[com.github.scala.android.crud.action.ContextVars]].
  * @author Eric Pabst (epabst@gmail.com)
  */

@RunWith(classOf[JUnitRunner])
class ContextVarsSpec extends Spec with MustMatchers with MockitoSugar {
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

  describe("getOrSet") {
    object StringVar extends ContextVar[String]
    trait Computation {
      def evaluate: String
    }

    it("must evaluate and set if not set yet") {
      val computation = mock[Computation]
      when(computation.evaluate).thenReturn("result")
      val vars = new ContextVars {}
      StringVar.getOrSet(vars, computation.evaluate) must be ("result")
      verify(computation).evaluate
    }

    it("must evaluate only the first time") {
      val computation = mock[Computation]
      when(computation.evaluate).thenReturn("result")
      val vars = new ContextVars {}
      StringVar.getOrSet(vars, computation.evaluate)
      StringVar.getOrSet(vars, computation.evaluate) must be ("result")
      verify(computation, times(1)).evaluate
    }

    it("must not evaluate if already set") {
      val vars = new ContextVars {}
      StringVar.set(vars, "hello")
      StringVar.getOrSet(vars, throw new IllegalArgumentException("shouldn't happen")) must be ("hello")
    }
  }
}
