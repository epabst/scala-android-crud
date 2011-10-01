package com.github.scala.android.crud

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.Spec

/**
 * A behavior specification for {@link CrudContext}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 4/5/11
 * Time: 7:59 PM
 */

@RunWith(classOf[JUnitRunner])
class CrudContextSpec extends Spec with MustMatchers {

  describe("ContextVar") {
    it("must retain its value for the same CrudContext") {
      object myVar extends ContextVar[String]
      val crudContext = new CrudContext(null, null)
      val crudContext2 = new CrudContext(null, null)
      myVar.get(crudContext) must be (None)
      myVar.set(crudContext, "hello")
      myVar.get(crudContext) must be (Some("hello"))
      myVar.get(crudContext2) must be (None)
      myVar.get(crudContext) must be (Some("hello"))
    }

    it("clear must clear the value for the same CrudContext") {
      object myVar extends ContextVar[String]
      val myVar2 = new ContextVar[String]
      val crudContext = new CrudContext(null, null)
      val crudContext2 = new CrudContext(null, null)
      myVar.set(crudContext, "hello")
      myVar2.set(crudContext, "howdy")

      myVar.clear(crudContext2) must be (None)
      myVar.get(crudContext) must be (Some("hello"))

      myVar.clear(crudContext) must be (Some("hello"))
      myVar.get(crudContext) must be (None)

      myVar2.clear(crudContext) must be (Some("howdy"))
    }
  }
}