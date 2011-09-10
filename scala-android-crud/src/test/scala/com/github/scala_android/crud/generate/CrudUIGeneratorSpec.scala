package com.github.scala_android.crud.generate

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.github.triangle.{PortableField, FieldList}
import PortableField._
import com.github.scala_android.crud.view.ViewField._

/**
 * A behavior specification for {@link CrudUIGenerator}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 8/6/11
 * Time: 8:13 AM
 */
@RunWith(classOf[JUnitRunner])
class CrudUIGeneratorSpec extends Spec with MustMatchers {
  describe("viewFields") {
    it("must find all fields whose subject is a View class") {
      val fieldList = FieldList(mapField[String]("foo"), textView, formatted[Int](textView),
        viewId(45, formatted[Double](textView)), datePicker)
      val fields = CrudUIGenerator.viewFields(fieldList)
      fields must be (List(textView, textView, textView, datePicker))
    }
  }

  describe("detectResourceIdClasses") {
    it("must be able to find all of the R.id instances") {
      CrudUIGenerator.detectResourceIdClasses(classOf[CrudUIGeneratorSpec]) must
              be (Seq(classOf[R.id], classOf[android.R.id], classOf[com.github.scala_android.crud.res.R.id]))
    }

    it("must look in parent packages to find the application R.id instance") {
      CrudUIGenerator.detectResourceIdClasses(classOf[foo.A]) must
              be (Seq(classOf[R.id], classOf[android.R.id], classOf[com.github.scala_android.crud.res.R.id]))
    }
  }

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

object R {
  class id {
    val foo: Int = 123
  }
}

package foo { class A }
