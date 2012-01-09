package com.github.scala.android.crud.generate

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.github.triangle.{PortableField, ValueFormat}
import PortableField._
import com.github.scala.android.crud.view.ViewField._
import com.github.scala.android.crud.ParentField._
import com.github.scala.android.crud.testres.R
import com.github.scala.android.crud._
import org.scalatest.mock.MockitoSugar
import testres.R.id
import view.EntityView

/** A behavior specification for [[com.github.scala.android.crud.generate.EntityFieldInfo]].
  * @author Eric Pabst (epabst@gmail.com)
  */
@RunWith(classOf[JUnitRunner])
class EntityFieldInfoSpec extends Spec with MustMatchers with MockitoSugar {
  describe("viewFields") {
    it("must find all ViewFields") {
      val dummyFormat = ValueFormat[String](s => Some(s + "."), _.stripSuffix("."))
      val fieldList = mapField[String]("foo") + textView + formatted[String](dummyFormat, textView) + viewId(45, textView)
      val info = ViewIdFieldInfo("foo", fieldList)
      info.viewFields must be(List(textView, textView, textView))
    }
  }

  it("must handle a viewId name that does not exist") {
    val fieldInfo = EntityFieldInfo(viewId(classOf[R.id], "bogus", textView), List(classOf[R])).viewIdFieldInfos.head
    fieldInfo.id must be ("bogus")
  }

  it("must consider a ParentField displayable if it has a viewId field") {
    val fieldInfo = EntityFieldInfo(ParentField(MyEntityType) + viewId(classOf[R], "foo", longView), Seq(classOf[R]))
    fieldInfo.isDisplayable must be (true)
  }

  it("must not include a ParentField if it has no viewId field") {
    val fieldInfos = EntityFieldInfo(ParentField(MyEntityType), Seq(classOf[R])).viewIdFieldInfos
    fieldInfos must be (Nil)
  }

  it("must not include adjustment fields") {
    val fieldInfos = EntityFieldInfo(adjustment[String](_ + "foo"), Seq(classOf[R])).viewIdFieldInfos
    fieldInfos must be (Nil)
  }

  it("must not include adjustmentInPlace fields") {
    val fieldInfos = EntityFieldInfo(adjustmentInPlace[StringBuffer] { s => s.append("foo"); Unit }, Seq(classOf[R])).viewIdFieldInfos
    fieldInfos must be (Nil)
  }

  it("must not include the default primary key field") {
    val fieldInfos = EntityFieldInfo(MyCrudType.entityType.IdField, Seq(classOf[R])).viewIdFieldInfos
    fieldInfos must be (Nil)
  }

  it("must not include a ForeignKey if it has no viewId field") {
    val fieldInfo = EntityFieldInfo(foreignKey(MyEntityType), Seq(classOf[R]))
    fieldInfo.isUpdateable must be (false)
  }

  it("must detect multiple ViewFields in the same field") {
    val fieldInfos = EntityFieldInfo(viewId(R.id.foo, textView) + viewId(R.id.bar, textView), Seq(classOf[R.id])).viewIdFieldInfos
    fieldInfos.map(_.id) must be (List("foo", "bar"))
  }

  val entityFieldInfo = EntityFieldInfo(viewId(R.id.foo, foreignKey(MyEntityType) + EntityView(MyEntityType)), Seq(classOf[id]))

  describe("updateableViewIdFieldInfos") {
    it("must provide a single field for an EntityView field to allow choosing Entity instance") {
      val fieldInfos = entityFieldInfo.updateableViewIdFieldInfos
      fieldInfos.map(_.id) must be (List("foo"))
      fieldInfos.map(_.layout).head.editXml.head.label must be ("Spinner")
    }
  }

  describe("displayableViewIdFieldInfos") {
    it("must provide each displayable field in the referenced EntityType for an EntityView field") {
      val fieldInfos = entityFieldInfo.displayableViewIdFieldInfos
      fieldInfos must be (EntityTypeViewInfo(MyEntityType).displayableViewIdFieldInfos)
    }
  }
}
