package com.github.triangle

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.Spec
import com.github.triangle.PortableField._
import org.scalatest.mock.EasyMockSugar
import collection.{immutable, mutable}
import mutable.Buffer
import collection.JavaConversions._


/**
 * A behavior specification for {@link PortableField}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/9/11
 * Time: 7:59 PM
 */

@RunWith(classOf[JUnitRunner])
class PortableFieldSpec extends Spec with MustMatchers with EasyMockSugar {
  describe("PortableField") {
    class MyEntity(var string: String, var number: Int)
    class OtherEntity(var name: String, var boolean: Boolean)

    it("must be easily instantiable for an Entity") {
      val a1 = fieldDirect[MyEntity,String](_.string, _.string_=)
      val a2 = fieldDirect[MyEntity,Int](_.number, _.number_=)
      val stringField =
        fieldDirect[MyEntity,String](_.string, _.string_=) +
        readOnly[OtherEntity,String](_.name) +
        writeOnlyDirect[OtherEntity,String](_.name_=)
      val intField = fieldDirect[MyEntity,Int](_.number, _.number_=)
      val readOnlyField = readOnly[MyEntity,Int](_.number)
    }

    describe("copy") {
      it("must set defaults") {
        val stringField = fieldDirect[MyEntity,String](_.string, _.string_=) + default("Hello")

        val myEntity1 = new MyEntity("my1", 15)
        stringField.copy(Unit, myEntity1)
        myEntity1.string must be ("Hello")
        myEntity1.number must be (15)
        //shouldn't fail
        stringField.copy(myEntity1, Unit)
      }

      it("must happen if getter is applicable") {
        val stringField = default("Hello") + mapField("greeting")
        val map = mutable.Map[String,Any]("greeting" -> "Hola")
        stringField.getter(Unit) must be (Some("Hello"))
        stringField.copy(Unit, map)
        map.get("greeting") must be (Some("Hello"))
      }

      it("must not use the setter if getter isn't applicable") {
        val stringField = default("Hello") + mapField("greeting")
        val map = mutable.Map[String,Any]("greeting" -> "Hola")
        stringField.getter.isDefinedAt(new Object) must be (false)
        stringField.copy(new Object, map) //does nothing
        map.get("greeting") must be (Some("Hola"))
      }

      it("must copy from one to multiple") {
        val stringField =
          readOnly[OtherEntity,String](_.name) +
          writeOnlyDirect[OtherEntity, String](_.name_=) +
          fieldDirect[MyEntity,String](_.string, _.string_=)

        val myEntity1 = new MyEntity("my1", 1)
        val otherEntity1 = new OtherEntity("other1", false)
        stringField.copy(myEntity1, otherEntity1)
        myEntity1.string must be ("my1")
        myEntity1.number must be (1)
        otherEntity1.name must be ("my1")
        otherEntity1.boolean must be (false)

        val otherEntity2 = new OtherEntity("other2", true)
        val myEntity2 = new MyEntity("my2", 2)
        stringField.copy(otherEntity2, myEntity2)
        otherEntity2.name must be ("other2")
        otherEntity2.boolean must be (true)
        myEntity2.string must be ("other2")
        myEntity2.number must be (2)

        stringField.copy(new Object, myEntity2) //does nothing

        stringField.copy(otherEntity2, new Object) //does nothing
      }
    }

    describe("default") {
      it("must only work on Unit") {
        val stringField = default("Hello")
        stringField.getter.isDefinedAt(List("bogus list")) must be (false)
        stringField.getter(Unit) must be (Some("Hello"))
      }
    }

    describe("mapField") {
      it("must clear") {
        val stringField = mapField[String]("greeting")
        val map = mutable.Map("greeting" -> "Hola")
        stringField.getter(mutable.Map[String,Any]()) must be (None)
        stringField.copy(mutable.Map[String,Any](), map)
        map.contains("greeting") must be (false)
      }
    }

    describe("copyFrom") {
      it("must return a working PortableValue if getter isDefinedAt") {
        val stringField = default("Hello") + mapField("greeting")
        stringField.getter(Unit) must be (Some("Hello"))
        val portableValue: PortableValue = stringField.copyFrom(Unit)
        portableValue must not(be(null))

        val map = mutable.Map[String,Any]()
        portableValue.copyTo(map)
        map.get("greeting") must be (Some("Hello"))
      }

      it("must return a no-op PortableValue if getter !isDefinedAt") {
        val stringField = default("Hello") + mapField("greeting")
        val portableValue: PortableValue = stringField.copyFrom("string")
        portableValue must not(be(null))

        val map = mutable.Map[String,Any]()
        portableValue.copyTo(map)
        map.get("greeting") must be (None)
      }
    }

    describe("copyFromItem") {
      it("must use the first applicable field variation with the first applicable item") {
        val myEntity1 = new MyEntity("my1", 1)
        val otherEntity1 = new OtherEntity("other1", false)
        val stringField = mapField[String]("stringValue") +
          fieldDirect[OtherEntity,String](_.name, _.name_=) +
          fieldDirect[MyEntity,String](_.string, _.string_=)
        stringField.getterFromItem.isDefinedAt(List(myEntity1, otherEntity1)) must be (true)
        stringField.getterFromItem.isDefinedAt(List(new Object)) must be (false)
        stringField.getterFromItem(List(myEntity1, otherEntity1)) must be (Some("other1"))
        stringField.getterFromItem(List(otherEntity1, myEntity1)) must be (Some("other1"))
        val mutableMap = mutable.Map.empty[String, Any]
        stringField.copyFromItem(List(myEntity1, otherEntity1), mutableMap)
        mutableMap("stringValue") must be ("other1")
      }
    }

    describe("getterFromItem") {
      it("must get from the first applicable item with Some value") {
        val fieldWithDefault = default(12) + mapField[Int]("count")
        fieldWithDefault.getterFromItem(List(immutable.Map.empty[String,Any], Unit)) must be (Some(12))
        fieldWithDefault.getterFromItem(List(immutable.Map.empty[String,Any])) must be (None)

        val fieldWithoutDefault = mapField[Int]("count")
        fieldWithoutDefault.getterFromItem(List(immutable.Map.empty[String,Any], Unit)) must be (None)

        val fieldWithDeprecatedName = mapField[Int]("count") + mapField[Int]("size")
        fieldWithDeprecatedName.getterFromItem(List(immutable.Map[String,Any]("size" -> 4))) must be (Some(4))
      }
    }

    describe("writeOnlyDirect") {
      it("must call clearer if no value") {
        val stringField = writeOnlyDirect[Buffer[String],String]({ b => v => b += v; Unit }, _.clear())
        val buffer = Buffer("hello")
        stringField.setValue(buffer, None)
        buffer must be ('empty)
      }
    }

    describe("formatted") {
      it("must parse and format values") {
        val field = formatted[Double](ValueFormat.currencyValueFormat, mapField[String]("amountString"))
        field(Map("amountString" -> "12.34")) must be (12.34)

        val map = mutable.Map[String,Any]()
        field.setValue(map, Some(16))
        map("amountString") must be ("16.00")
      }

      it("must have a working transformer") {
        val formattedField = formatted[Int](mapField[String]("countString"))
        //qualified to point out that it's immutable
        val result = formattedField.transformer(immutable.Map.empty[String,Int])(4)
        result.get("countString") must be (Some("4"))
      }
    }

    describe("transformer") {
      it("must delegate to setter for mutable objects") {
        val stringField = mapField[String]("greeting")
        val map = mutable.Map[String,Any]()
        val result = stringField.transformer(map)(Some("hello"))
        result.get("greeting") must be (Some("hello"))
        result must be (map)
      }

      it("must support transforming immutable objects") {
        val stringField = mapField[String]("greeting")
        val intField = mapField[Int]("count")
        //qualified to point out that it's immutable
        val result = stringField.transformer(immutable.Map.empty[String,Any])(Some("hello"))
        result.get("greeting") must be (Some("hello"))

        val result2 = intField.transformer(result)(Some(10))
        result2.get("greeting") must be (Some("hello"))
        result2.get("count") must be (Some(10))
      }

      it("must use all transformers of a field") {
        val stringField = mapField[String]("greeting") +
                transformOnlyDirect[immutable.Map[String,String],String](map => ignored => map + ("greeting" -> map("greeting").toUpperCase), map => map)
        //qualified to point out that it's immutable
        val result = stringField.transformer(immutable.Map.empty[String,String])(Some("hello"))
        result.get("greeting") must be (Some("HELLO"))
      }
    }

    describe("transform") {
      it("must use an initial and some data") {
        val stringField = mapField[String]("greeting") +
                transformOnlyDirect[immutable.Map[String,String],String](map => ignored => map + ("greeting" -> map("greeting").toUpperCase), map => map)
        //qualified to point out that it's immutable
        val result = stringField.transform(initial = immutable.Map.empty[String,String], data = immutable.Map("greeting" -> "hello", "ignored" -> "foo"))
        result must be (immutable.Map[String,String]("greeting" -> "HELLO"))
      }

      it("must not transform if initial subject is not applicable") {
        val stringField = mapField[String]("greeting")
        val result = stringField.transform(initial = "inapplicable data", data = immutable.Map("greeting" -> "hello", "ignored" -> "foo"))
        result must be ("inapplicable data")
      }

      it("must not transform if data is not applicable") {
        val stringField = mapField[String]("greeting")
        val result = stringField.transform(initial = immutable.Map.empty[String,String], data = "inapplicable data")
        result must be (immutable.Map.empty[String,String])
      }
    }

    describe("transformWithItem") {
      it("must transform using the applicable item") {
        val countField = default(12) + mapField[Int]("count")
        val result = countField.transformWithItem(initial = immutable.Map.empty[String,Any],
                                                  dataItems = List(new Object, Unit))
        result must be (immutable.Map[String,Any]("count" -> 12))
      }
    }

    describe("+") {
      it("must use getterForItem on each Field added together") {
        val mockField = mock[PortableField[String]]
        val field = mapField[String]("foo") + mockField
        expecting {
          call(mockField.getterFromItem).andReturn({
            case List(Unit, "String") => Some("success")
          }).anyTimes
        }
        whenExecuting(mockField) {
          field.getterFromItem(List(Unit, "String")) must be (Some("success"))
        }
      }
    }
  }
}
