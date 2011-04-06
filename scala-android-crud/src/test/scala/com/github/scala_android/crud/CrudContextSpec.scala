package com.github.scala_android.crud

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.Spec

/**
 * A behavior specification for {@link CrudContext}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 4/5/11
 * Time: 7:59 PM
 */

@RunWith(classOf[JUnitRunner])
class CrudContextSpec extends Spec with ShouldMatchers {

  describe("ContextVar") {
    it("should retain its value for the same CrudContext") {
      object myVar extends ContextVar[String]
      val crudContext = new CrudContext(null)
      val crudContext2 = new CrudContext(null)
      myVar.get(crudContext) should be (None)
      myVar.set(crudContext, "hello")
      myVar.get(crudContext) should be (Some("hello"))
      myVar.get(crudContext2) should be (None)
      myVar.get(crudContext) should be (Some("hello"))
    }

    it("clear should clear the value for the same CrudContext") {
      object myVar extends ContextVar[String]
      object myVar2 extends ContextVar[String]
      val crudContext = new CrudContext(null)
      val crudContext2 = new CrudContext(null)
      myVar.set(crudContext, "hello")
      myVar2.set(crudContext, "howdy")

      myVar.clear(crudContext2)
      myVar.get(crudContext) should be (Some("hello"))

      myVar.clear(crudContext)
      myVar.get(crudContext) should be (None)
      myVar2.get(crudContext) should be (Some("howdy"))
    }
  }
}