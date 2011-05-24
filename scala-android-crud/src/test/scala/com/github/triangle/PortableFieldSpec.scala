package com.github.triangle

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.Spec
import com.github.triangle.PortableField._
import scala.collection.{immutable,mutable}
import mutable.Buffer
import org.scalatest.mock.EasyMockSugar


/**
 * A behavior specification for {@link PortableField}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/9/11
 * Time: 7:59 PM
 */

@RunWith(classOf[JUnitRunner])
class PortableFieldSpec extends Spec with ShouldMatchers with EasyMockSugar {
  describe("PortableField") {
    class MyEntity(var string: String, var number: Int)
    class OtherEntity(var name: String, var boolean: Boolean)

    it("should be easily instantiable for an Entity") {
      val a1 = fieldDirect[MyEntity,String](_.string, _.string_=)
      val a2 = fieldDirect[MyEntity,Int](_.number, _.number_=)
      val stringField =
        fieldDirect[MyEntity,String](_.string, _.string_=) +
        readOnly[OtherEntity,String](_.name) +
        writeOnlyDirect[OtherEntity,String](_.name_=)
      val intField = fieldDirect[MyEntity,Int](_.number, _.number_=)
      val readOnlyField = readOnly[MyEntity,Int](_.number)
    }

    it("should set defaults") {
      val stringField = fieldDirect[MyEntity,String](_.string, _.string_=) + default("Hello")

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
        writeOnlyDirect[OtherEntity, String](_.name_=) +
        fieldDirect[MyEntity,String](_.string, _.string_=)

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

    it("should use the first applicable field variation with the first applicable item") {
      val myEntity1 = new MyEntity("my1", 1)
      val otherEntity1 = new OtherEntity("other1", false)
      val stringField = mapField[String]("stringValue") +
        fieldDirect[OtherEntity,String](_.name, _.name_=) +
        fieldDirect[MyEntity,String](_.string, _.string_=)
      stringField.getterFromItem.isDefinedAt(List(myEntity1, otherEntity1)) should be (true)
      stringField.getterFromItem.isDefinedAt(List(new Object)) should be (false)
      stringField.getterFromItem(List(myEntity1, otherEntity1)) should be (Some("other1"))
      stringField.getterFromItem(List(otherEntity1, myEntity1)) should be (Some("other1"))
      stringField.getFromItemOrReturn(List(myEntity1), Some("n/a")) should be (Some("my1"))
      stringField.getFromItemOrReturn(List(new Object), Some("n/a")) should be (Some("n/a"))
      val mutableMap = mutable.Map.empty[String, Any]
      stringField.copyFromItem(List(myEntity1, otherEntity1), mutableMap) should be (true)
      mutableMap("stringValue") should be ("other1")
    }

    it("should get from the first applicable item with Some value") {
      val fieldWithDefault = default(12) + mapField[Int]("count")
      fieldWithDefault.getterFromItem(List(immutable.Map.empty[String,Any], Unit)) should be (Some(12))
      fieldWithDefault.getterFromItem(List(immutable.Map.empty[String,Any])) should be (None)

      val fieldWithoutDefault = mapField[Int]("count")
      fieldWithoutDefault.getterFromItem(List(immutable.Map.empty[String,Any], Unit)) should be (None)

      val fieldWithDeprecatedName = mapField[Int]("count") + mapField[Int]("size")
      fieldWithDeprecatedName.getterFromItem(List(immutable.Map[String,Any]("size" -> 4))) should be (Some(4))
    }

    it("writeOnlyDirect should call clearer if no value") {
      val stringField = writeOnlyDirect[Buffer[String],String]({ b => v => b += v; Unit }, _.clear())
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

    it("transformer should delegate to setter for mutable objects") {
      val stringField = mapField[String]("greeting")
      val map = mutable.Map[String,Any]()
      val result = stringField.transformer(map)(Some("hello"))
      result.get("greeting") should be (Some("hello"))
      result should be (map)
    }

    it("should support transforming immutable objects") {
      val stringField = mapField[String]("greeting")
      val intField = mapField[Int]("count")
      //qualified to point out that it's immutable
      val result = stringField.transformer(immutable.Map.empty[String,Any])(Some("hello"))
      result.get("greeting") should be (Some("hello"))

      val result2 = intField.transformer(result)(Some(10))
      result2.get("greeting") should be (Some("hello"))
      result2.get("count") should be (Some(10))
    }

    it("all transformers of a field should be used") {
      val stringField = mapField[String]("greeting") +
              transformOnlyDirect[immutable.Map[String,String],String](map => ignored => map + ("greeting" -> map("greeting").toUpperCase), map => map)
      //qualified to point out that it's immutable
      val result = stringField.transformer(immutable.Map.empty[String,String])(Some("hello"))
      result.get("greeting") should be (Some("HELLO"))
    }

    it("should transform using an initial and some data") {
      val stringField = mapField[String]("greeting") +
              transformOnlyDirect[immutable.Map[String,String],String](map => ignored => map + ("greeting" -> map("greeting").toUpperCase), map => map)
      //qualified to point out that it's immutable
      val result = stringField.transform(initial = immutable.Map.empty[String,String], data = immutable.Map("greeting" -> "hello", "ignored" -> "foo"))
      result should be (immutable.Map[String,String]("greeting" -> "HELLO"))
    }

    it("should not transform if initial subject is not applicable") {
      val stringField = mapField[String]("greeting")
      val result = stringField.transform(initial = "inapplicable data", data = immutable.Map("greeting" -> "hello", "ignored" -> "foo"))
      result should be ("inapplicable data")
    }

    it("should not transform if data is not applicable") {
      val stringField = mapField[String]("greeting")
      val result = stringField.transform(initial = immutable.Map.empty[String,String], data = "inapplicable data")
      result should be (immutable.Map.empty[String,String])
    }

    it("should transform using the applicable item") {
      val countField = default(12) + mapField[Int]("count")
      val result = countField.transformWithItem(initial = immutable.Map.empty[String,Any],
                                                dataItems = List(new Object, Unit))
      result should be (immutable.Map[String,Any]("count" -> 12))
    }

    it("formatted transformer should work") {
      val formattedField = formatted[Int](mapField[String]("countString"))
      //qualified to point out that it's immutable
      val result = formattedField.transformer(immutable.Map.empty[String,Int])(4)
      result.get("countString") should be (Some("4"))
    }

    it("should use getterForItem on each Field added together") {
      val mockField = mock[PortableField[String]]
      val field = mapField[String]("foo") + mockField
      expecting {
        call(mockField.getterFromItem).andReturn({
          case List(Unit, "String") => Some("success")
        }).anyTimes
      }
      whenExecuting(mockField) {
        field.getterFromItem(List(Unit, "String")) should be (Some("success"))
      }
    }
  }
}
