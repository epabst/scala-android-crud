package com.github.triangle

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.Spec
import com.github.triangle.PortableField._
import scala.collection.mutable
import mutable.Buffer


/**
 * A behavior specification for {@link PortableField}, {@link CursorField}, and {@link ViewField}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/9/11
 * Time: 7:59 PM
 */

@RunWith(classOf[JUnitRunner])
class PortableFieldSpec extends Spec with ShouldMatchers {
  describe("PortableField") {
    class MyEntity(var string: String, var number: Int)
    class OtherEntity(var name: String, var boolean: Boolean)

    it("should be easily instantiable for an Entity") {
      val a1 = field[MyEntity,String](_.string, _.string_=)
      val a2 = field[MyEntity,Int](_.number, _.number_=)
      val stringField =
        flow[MyEntity, OtherEntity, String](_.string, _.name_=) +
        field[MyEntity,String](_.string, _.string_=) +
        readOnly[OtherEntity,String](_.name) +
        writeOnly[MyEntity,String](_.string_=) +
        field[OtherEntity,String](_.name, _.name_=)
      val intField = field[MyEntity,Int](_.number, _.number_=)
      val readOnlyField = readOnly[MyEntity,Int](_.number)
    }

    it("should set defaults") {
      val stringField = field[MyEntity,String](_.string, _.string_=) + default("Hello")

      val myEntity1 = new MyEntity("my1", 15)
      stringField.copy(Unit, myEntity1) should be (true)
      myEntity1.string should be ("Hello")
      myEntity1.number should be (15)
      //shouldn't fail
      stringField.copy(myEntity1, Unit) should be (false)
    }

    it("default should only work on Unit") {
      val stringField = default("Hello")
      stringField.getter.isDefinedAt(List("bogus list")) should be (false)
      stringField.getter(Unit) should be (Some("Hello"))
    }

    it("mapField should clear") {
      val stringField = mapField[String]("greeting")
      val map = mutable.Map("greeting" -> "Hola")
      stringField.getter(mutable.Map[String,Any]()) should be (None)
      stringField.copy(mutable.Map[String,Any](), map) should be (true)
      map.get("greeting") should be (None)
    }

    it("copy should happen if getter is applicable") {
      val stringField = default("Hello") + mapField("greeting")
      val map = mutable.Map[String,Any]("greeting" -> "Hola")
      stringField.getter(Unit) should be (Some("Hello"))
      stringField.copy(Unit, map) should be (true)
      map.get("greeting") should be (Some("Hello"))
    }

    it("setter should not be used if getter isn't applicable") {
      val stringField = default("Hello") + mapField("greeting")
      val map = mutable.Map[String,Any]("greeting" -> "Hola")
      stringField.getter.isDefinedAt(new Object) should be (false)
      stringField.copy(new Object, map) should be (false)
      map.get("greeting") should be (Some("Hola"))
    }

    it("should copy from one to multiple") {
      val stringField =
        readOnly[OtherEntity,String](_.name) +
        flow[MyEntity, OtherEntity, String](_.string, _.name_=) +
        field[MyEntity,String](_.string, _.string_=)

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

    it("should get from the first applicable item") {
      val myEntity1 = new MyEntity("my1", 1)
      val otherEntity1 = new OtherEntity("other1", false)
      val stringField = mapField[String]("stringValue") +
        field[OtherEntity,String](_.name, _.name_=) +
        field[MyEntity,String](_.string, _.string_=)
      stringField.getterFromItem.isDefinedAt(List(myEntity1, otherEntity1)) should be (true)
      stringField.getterFromItem.isDefinedAt(List(new Object)) should be (false)
      stringField.getterFromItem(List(myEntity1, otherEntity1)) should be (Some("my1"))
      stringField.getterFromItem(List(otherEntity1, myEntity1)) should be (Some("other1"))
      stringField.getFromItemOrReturn(List(myEntity1), Some("n/a")) should be (Some("my1"))
      stringField.getFromItemOrReturn(List(new Object), Some("n/a")) should be (Some("n/a"))
      val mutableMap = mutable.Map.empty[String, Any]
      stringField.copyFromItem(List(myEntity1, otherEntity1), mutableMap) should be (true)
      mutableMap("stringValue") should be ("my1")
    }

    it("writeOnly should call clearer if no value") {
      val stringField = writeOnly[Buffer[String],String]({ b => v => b += v; Unit }, _.clear())
      val buffer = Buffer("hello")
      stringField.setValue(buffer, None)
      buffer should be ('empty)
    }

    it("should parse and format values") {
      val field = formatted[Double](ValueFormat.currencyValueFormat, mapField[String]("amountString"))
      field(Map("amountString" -> "12.34")) should be (12.34)

      val map = mutable.Map[String,Any]()
      field.setValue(map, Some(16))
      map("amountString") should be ("16.00")
    }
  }
}