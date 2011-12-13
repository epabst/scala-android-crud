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
import com.github.scala.android.crud.persistence.CursorField._
import com.github.scala.android.crud.view.FieldLayout
import com.github.scala.android.crud._
import org.scalatest.mock.MockitoSugar

/**
 * A behavior specification for {@link CrudUIGenerator}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 8/6/11
 * Time: 8:13 AM
 */
@RunWith(classOf[JUnitRunner])
class CrudUIGeneratorSpec extends Spec with MustMatchers with MockitoSugar {
  describe("viewFields") {
    it("must find all ViewFields") {
      val fieldList = FieldList(mapField[String]("foo"), textView, formatted[Int](textView),
        viewId(45, formatted[Double](textView)), dateView)
      val fields = CrudUIGenerator.viewFields(fieldList)
      fields must be(List(textView, textView, textView, dateView))
    }
  }

  describe("guessFieldInfos") {
    it("must handle a viewId name that does not exist") {
      val fieldInfo = CrudUIGenerator.guessFieldInfos(viewId(classOf[R.id], "bogus", textView), List(classOf[R])).head
      fieldInfo.id must be ("bogus")
    }

    it("must consider a ParentField displayable if it has a viewId field") {
      val fieldInfo = CrudUIGenerator.guessFieldInfos(ParentField(MyEntityType) + viewId(classOf[R], "foo", longView), Seq(classOf[R])).head
      fieldInfo.displayable must be (true)
    }

    it("must not include a ParentField if it has no viewId field") {
      val fieldInfos = CrudUIGenerator.guessFieldInfos(ParentField(MyEntityType), Seq(classOf[R]))
      fieldInfos must be (Nil)
    }

    it("must not include adjustment fields") {
      val fieldInfos = CrudUIGenerator.guessFieldInfos(adjustment[String](_ + "foo"), Seq(classOf[R]))
      fieldInfos must be (Nil)
    }

    it("must not include adjustmentInPlace fields") {
      val fieldInfos = CrudUIGenerator.guessFieldInfos(adjustmentInPlace[StringBuffer] { s => s.append("foo"); Unit }, Seq(classOf[R]))
      fieldInfos must be (Nil)
    }

    it("must not include the default primary key field") {
      val fieldInfos = CrudUIGenerator.guessFieldInfos(MyCrudType.entityType.IdField, Seq(classOf[R]))
      fieldInfos must be (Nil)
    }

    it("must not include a ForeignKey if it has no viewId field") {
      val fieldInfo = CrudUIGenerator.guessFieldInfos(foreignKey(MyEntityType), Seq(classOf[R])).head
      fieldInfo.updateable must be (false)
    }

    it("must detect multiple ViewFields in the same field") {
      val fieldInfos = CrudUIGenerator.guessFieldInfos(viewId(R.id.foo, textView) + viewId(R.id.bar, textView), Seq(classOf[R.id]))
      fieldInfos.map(_.id) must be (List("foo", "bar"))
    }
  }

  describe("fieldLayoutForHeader") {
    it("must show the display name") {
      val position = 0
      val fieldLayout = CrudUIGenerator.fieldLayoutForHeader(ViewFieldInfo("My Name", FieldLayout.nameLayout, "foo", true, true), position)
      fieldLayout.attributes.find(_.key == "text").get.value.text must be ("My Name")
    }

    it("must put the first field on the left side of the screen") {
      val position = 0
      val fieldLayout = CrudUIGenerator.fieldLayoutForHeader(ViewFieldInfo("Foo", FieldLayout.nameLayout, "foo", true, true), position)
      fieldLayout.attributes.find(_.key == "layout_width").get.value.text must be ("wrap_content")
      fieldLayout.attributes.find(_.key == "gravity").get.value.text must be ("left")
    }

    it("must put the second field on the right side of the screen") {
      val position = 1
      val fieldLayout = CrudUIGenerator.fieldLayoutForHeader(ViewFieldInfo("Foo", FieldLayout.nameLayout, "foo", true, true), position)
      fieldLayout.attributes.find(_.key == "layout_width").get.value.text must be ("fill_parent")
      fieldLayout.attributes.find(_.key == "gravity").get.value.text must be ("right")
    }
  }

  describe("fieldLayoutForRow") {
    it("must put the first field on the left side of the screen") {
      val position = 0
      val fieldLayout = CrudUIGenerator.fieldLayoutForRow(ViewFieldInfo("Foo", FieldLayout.nameLayout, "foo", true, true), position)
      fieldLayout.attributes.find(_.key == "layout_width").get.value.text must be ("wrap_content")
      fieldLayout.attributes.find(_.key == "gravity").get.value.text must be ("left")
    }

    it("must put the second field on the right side of the screen") {
      val position = 1
      val fieldLayout = CrudUIGenerator.fieldLayoutForRow(ViewFieldInfo("Foo", FieldLayout.nameLayout, "foo", true, true), position)
      fieldLayout.attributes.find(_.key == "layout_width").get.value.text must be ("fill_parent")
      fieldLayout.attributes.find(_.key == "gravity").get.value.text must be ("right")
    }
  }

  describe("generateValueStrings") {
    it("must include 'list', 'add' and 'edit' strings for modifiable entities") {
      val valueStrings = CrudUIGenerator.generateValueStrings(new MyCrudType(new MyEntityType {
        override def valueFields = List(persisted[String]("model"))
      }))
      valueStrings.foreach(println(_))
      (valueStrings \\ "string").length must be (3)
    }

    it("must not include 'add' and 'edit' strings for unmodifiable entities") {
      (CrudUIGenerator.generateValueStrings(new MyCrudType(new MyEntityType {
        override def valueFields = List(bundleField[String]("model"))
      })) \\ "string").length must be (1)
    }
  }
}
