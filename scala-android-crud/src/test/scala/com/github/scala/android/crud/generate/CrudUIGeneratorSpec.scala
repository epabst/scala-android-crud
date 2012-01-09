package com.github.scala.android.crud.generate

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.github.scala.android.crud.persistence.CursorField._
import com.github.scala.android.crud._
import org.scalatest.mock.MockitoSugar
import view.ViewField
import ViewField._

/** A behavior specification for [[com.github.scala.android.crud.generate.CrudUIGenerator]].
  * @author Eric Pabst (epabst@gmail.com)
  */
@RunWith(classOf[JUnitRunner])
class CrudUIGeneratorSpec extends Spec with MustMatchers with MockitoSugar {
  val displayName = "My Name"
  val viewIdFieldInfo = ViewIdFieldInfo("foo", displayName, textView)

  describe("fieldLayoutForHeader") {
    it("must show the display name") {
      val position = 0
      val fieldLayout = CrudUIGenerator.fieldLayoutForHeader(viewIdFieldInfo, position)
      fieldLayout.attributes.find(_.key == "text").get.value.text must be ("My Name")
    }

    it("must put the first field on the left side of the screen") {
      val position = 0
      val fieldLayout = CrudUIGenerator.fieldLayoutForHeader(viewIdFieldInfo, position)
      fieldLayout.attributes.find(_.key == "layout_width").get.value.text must be ("wrap_content")
      fieldLayout.attributes.find(_.key == "gravity").get.value.text must be ("left")
    }

    it("must put the second field on the right side of the screen") {
      val position = 1
      val fieldLayout = CrudUIGenerator.fieldLayoutForHeader(viewIdFieldInfo, position)
      fieldLayout.attributes.find(_.key == "layout_width").get.value.text must be ("fill_parent")
      fieldLayout.attributes.find(_.key == "gravity").get.value.text must be ("right")
    }
  }

  describe("fieldLayoutForRow") {
    it("must put the first field on the left side of the screen") {
      val position = 0
      val fieldLayout = CrudUIGenerator.fieldLayoutForRow(viewIdFieldInfo, position)
      fieldLayout.head.attributes.find(_.key == "layout_width").get.value.text must be ("wrap_content")
      fieldLayout.head.attributes.find(_.key == "gravity").get.value.text must be ("left")
    }

    it("must put the second field on the right side of the screen") {
      val position = 1
      val fieldLayout = CrudUIGenerator.fieldLayoutForRow(viewIdFieldInfo, position)
      fieldLayout.head.attributes.find(_.key == "layout_width").get.value.text must be ("fill_parent")
      fieldLayout.head.attributes.find(_.key == "gravity").get.value.text must be ("right")
    }
  }

  describe("generateValueStrings") {
    it("must include 'list', 'add' and 'edit' strings for modifiable entities") {
      val valueStrings = CrudUIGenerator.generateValueStrings(new MyCrudType(new MyEntityType {
        override def valueFields = List(persisted[String]("model") + viewId(classOf[testres.R], "model", textView))
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
