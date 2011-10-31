package com.github.scala.android.crud.generate

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.github.triangle.{PortableField, FieldList}
import PortableField._
import com.github.scala.android.crud.view.ViewField._
import com.github.scala.android.crud.ParentField._
import com.github.scala.android.crud.testres.R
import com.github.scala.android.crud.persistence.{IdPk, SQLiteCriteria}
import com.github.scala.android.crud.persistence.CursorField._
import com.github.scala.android.crud.view.FieldLayout
import com.github.scala.android.crud._

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

    it("must consider a ParentField displayable if it has a viewId field") {
      val fieldInfo = CrudUIGenerator.guessFieldInfo(ParentField(MyCrudType) + viewId(classOf[R], "foo", longView), Seq(classOf[R]))
      fieldInfo.displayable must be (true)
    }

    it("must not consider a ParentField displayable if it has no viewId field") {
      val fieldInfo = CrudUIGenerator.guessFieldInfo(ParentField(MyCrudType), Seq(classOf[R]))
      fieldInfo.displayable must be (false)
    }

    it("must not consider adjustment fields displayable") {
      val fieldInfo = CrudUIGenerator.guessFieldInfo(adjustment[SQLiteCriteria](_.orderBy = "foo"), Seq(classOf[R]))
      fieldInfo.displayable must be (false)
    }

    it("must not consider the default primary key field displayable") {
      val fieldInfo = CrudUIGenerator.guessFieldInfo(IdPk.IdField, Seq(classOf[R]))
      fieldInfo.displayable must be (false)
    }

    it("must not consider the default primary key field updateable") {
      val fieldInfo = CrudUIGenerator.guessFieldInfo(IdPk.IdField, Seq(classOf[R]))
      fieldInfo.updateable must be (false)
    }

    it("must not consider a ForeignKey updateable if it has no viewId field") {
      val fieldInfo = CrudUIGenerator.guessFieldInfo(foreignKey(MyCrudType), Seq(classOf[R]))
      fieldInfo.updateable must be (false)
    }

    it("must not consider adjustment fields updateable") {
      val fieldInfo = CrudUIGenerator.guessFieldInfo(adjustment[SQLiteCriteria](_.orderBy = "foo"), Seq(classOf[R]))
      fieldInfo.updateable must be (false)
    }
  }

  describe("fieldLayoutForHeader") {
    it("must show the display name") {
      val position = 0
      val fieldLayout = CrudUIGenerator.fieldLayoutForHeader(ViewFieldInfo(Some("My Name"), FieldLayout.nameLayout, "foo", true, true), position)
      fieldLayout.attributes.find(_.key == "text").get.value.text must be ("My Name")
    }

    it("must put the first field on the left side of the screen") {
      val position = 0
      val fieldLayout = CrudUIGenerator.fieldLayoutForHeader(ViewFieldInfo(None, FieldLayout.nameLayout, "foo", true, true), position)
      fieldLayout.attributes.find(_.key == "layout_width").get.value.text must be ("wrap_content")
      fieldLayout.attributes.find(_.key == "gravity").get.value.text must be ("left")
    }

    it("must put the second field on the right side of the screen") {
      val position = 1
      val fieldLayout = CrudUIGenerator.fieldLayoutForHeader(ViewFieldInfo(None, FieldLayout.nameLayout, "foo", true, true), position)
      fieldLayout.attributes.find(_.key == "layout_width").get.value.text must be ("fill_parent")
      fieldLayout.attributes.find(_.key == "gravity").get.value.text must be ("right")
    }
  }

  describe("fieldLayoutForRow") {
    it("must put the first field on the left side of the screen") {
      val position = 0
      val fieldLayout = CrudUIGenerator.fieldLayoutForRow(ViewFieldInfo(None, FieldLayout.nameLayout, "foo", true, true), position)
      fieldLayout.attributes.find(_.key == "layout_width").get.value.text must be ("wrap_content")
      fieldLayout.attributes.find(_.key == "gravity").get.value.text must be ("left")
    }

    it("must put the second field on the right side of the screen") {
      val position = 1
      val fieldLayout = CrudUIGenerator.fieldLayoutForRow(ViewFieldInfo(None, FieldLayout.nameLayout, "foo", true, true), position)
      fieldLayout.attributes.find(_.key == "layout_width").get.value.text must be ("fill_parent")
      fieldLayout.attributes.find(_.key == "gravity").get.value.text must be ("right")
    }
  }

  describe("generateValueStrings") {
    it("must include 'list', 'add' and 'edit' strings for modifiable entities") {
      val valueStrings = CrudUIGenerator.generateValueStrings(new MyCrudType {
        override def valueFields = List(persisted[String]("model"))
      })
      valueStrings.foreach(println(_))
      (valueStrings \\ "string").length must be (3)
    }

    it("must not include 'add' and 'edit' strings for unmodifiable entities") {
      (CrudUIGenerator.generateValueStrings(new MyCrudType {
        override def valueFields = List(bundleField[String]("model"))
      }) \\ "string").length must be (1)
    }
  }
}
