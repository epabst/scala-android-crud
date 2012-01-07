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
import com.github.scala.android.crud._
import org.scalatest.mock.MockitoSugar

/** A behavior specification for [[com.github.scala.android.crud.generate.EntityFieldInfo]].
  * @author Eric Pabst (epabst@gmail.com)
  */
@RunWith(classOf[JUnitRunner])
class EntityFieldInfoSpec extends Spec with MustMatchers with MockitoSugar {
  describe("viewFields") {
    it("must find all ViewFields") {
      val fieldList = FieldList(mapField[String]("foo"), textView, formatted[Int](textView),
        viewId(45, formatted[Double](textView)), dateView)
      val info = EntityFieldInfo(null, Nil)
      val fields = info.viewFields(fieldList)
      fields must be(List(textView, textView, textView, dateView))
    }
  }

  it("must handle a viewId name that does not exist") {
    val fieldInfo = EntityFieldInfo(viewId(classOf[R.id], "bogus", textView), List(classOf[R])).viewFieldInfos.head
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
