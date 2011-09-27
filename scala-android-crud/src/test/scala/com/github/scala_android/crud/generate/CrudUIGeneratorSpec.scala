package com.github.scala_android.crud.generate

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.github.triangle.{PortableField, FieldList}
import PortableField._
import com.github.scala_android.crud.view.ViewField._
import com.github.scala_android.crud.testres.R
import com.github.scala_android.crud.persistence.SQLiteCriteria
import com.github.scala_android.crud.{MyCrudType, ParentField}

/**
 * A behavior specification for {@link CrudUIGenerator}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 8/6/11
 * Time: 8:13 AM
 */
@RunWith(classOf[JUnitRunner])
class CrudUIGeneratorSpec extends Spec with MustMatchers {
  describe("viewFields") {
    it("must find all ViewFields") {
      val fieldList = FieldList(mapField[String]("foo"), textView, formatted[Int](textView),
        viewId(45, formatted[Double](textView)), dateView)
      val fields = CrudUIGenerator.viewFields(fieldList)
      fields must be(List(textView, textView, textView, calendarDateView))
    }
  }

  describe("guessFieldInfo") {
    it("must handle a viewId name that does not exist") {
      val fieldInfo = CrudUIGenerator.guessFieldInfo(viewId(classOf[R.id], "bogus", textView), List(classOf[R]))
      fieldInfo.id must be ("bogus")
    }

    it("must display a ParentField if it has a viewId field") {
      val fieldInfo = CrudUIGenerator.guessFieldInfo(ParentField(MyCrudType) + viewId(classOf[R], "foo", longView), Seq(classOf[R]))
      fieldInfo.displayable must be (true)
    }

    it("must not display a ParentField if it has no viewId field") {
      val fieldInfo = CrudUIGenerator.guessFieldInfo(ParentField(MyCrudType), Seq(classOf[R]))
      fieldInfo.displayable must be (false)
    }

    it("must not display adjustment fields") {
      val fieldInfo = CrudUIGenerator.guessFieldInfo(adjustment[SQLiteCriteria](_.orderBy = "foo"), Seq(classOf[R]))
      fieldInfo.displayable must be (false)
    }
  }
}
