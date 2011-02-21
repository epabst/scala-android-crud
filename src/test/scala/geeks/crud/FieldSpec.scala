package geeks.crud

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.Spec
import java.text.NumberFormat
import geeks.crud.Field._
import geeks.crud.android.CursorFieldAccess._
import geeks.crud.android.ViewFieldAccess._
import _root_.android.view.View
import _root_.android.widget.TextView
import _root_.android.content.Context


/**
 * A behavior specification for {@link Field}, {@link CursorFieldAccess}, and {@link ViewFieldAccess}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/9/11
 * Time: 7:59 PM
 */

@RunWith(classOf[JUnitRunner])
class FieldSpec extends Spec with ShouldMatchers {
  describe("Field") {
    class MyEntity(var string: String, var number: Int)
    class OtherEntity(var name: String, var boolean: Boolean)

    it("should be easily instantiable for an Entity") {
      val a1 = fieldAccess[MyEntity,String](_.string, _.string_=)
      val a2 = fieldAccess[MyEntity,Int](_.number, _.number_=)
      val stringField = Field(
        flow[MyEntity, OtherEntity, String](_.string, _.name_=),
        persisted[String]("name"),
        fieldAccess[MyEntity,String](_.string, _.string_=),
        readOnly[OtherEntity,String](_.name),
        writeOnly[MyEntity,String](_.string_=),
        fieldAccess[OtherEntity,String](_.name, _.name_=))
      val intField = Field(fieldAccess[MyEntity,Int](_.number, _.number_=))
      val readOnlyField = Field(readOnly[MyEntity,Int](_.number))
    }

    it("should be easily instantiable for a View") {
      class MyView(context: Context, var status: String) extends View(context)

      val stringField = Field(
        persisted[String]("name"),
        viewId[TextView,String](101),
        viewFieldAccessById[MyView,String](102, _.status, _.status_=),
        viewId(102)(viewFieldAccess[MyView,String](_.status, _.status_=)))
    }

    it("should set defaults") {
      val stringField = Field(
        fieldAccess[MyEntity,String](_.string, _.string_=), default("Hello"))

      val myEntity1 = new MyEntity("my1", 15)
      stringField.copy(Unit, myEntity1) should be (true)
      myEntity1.string should be ("Hello")
      myEntity1.number should be (15)
      //shouldn't fail
      stringField.copy(myEntity1, Unit) should be (false)
    }

    it("should copy from one to multiple") {
      val stringField = Field(
        readOnly[OtherEntity,String](_.name),
        flow[MyEntity, OtherEntity, String](_.string, _.name_=),
        fieldAccess[MyEntity,String](_.string, _.string_=))

      val myEntity1 = new MyEntity("my1", 1)
      val otherEntity1 = new OtherEntity("other1", false)
      stringField.copy(myEntity1, otherEntity1) should be (true)
      myEntity1.string should be ("my1")
      myEntity1.number should be (1)
      otherEntity1.name should be ("my1")
      otherEntity1.boolean should be (false)

      val otherEntity2 = new OtherEntity("other2", true)
      val myEntity2 = new MyEntity("my2", 2)
      stringField.copy(otherEntity2, myEntity2) should be (true)
      otherEntity2.name should be ("other2")
      otherEntity2.boolean should be (true)
      myEntity2.string should be ("other2")
      myEntity2.number should be (2)

      stringField.copy(new Object, myEntity2) should be (false)

      stringField.copy(otherEntity2, new Object) should be (false)
    }
  }
}