package com.github.triangle

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.Spec
import java.text.NumberFormat
import ValueFormat._

/**
 * A behavior specification for {@link ValueFormat}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/9/11
 * Time: 7:59 PM
 */

@RunWith(classOf[JUnitRunner])
class ValueFormatSpec extends Spec with MustMatchers {
  describe("basicFormat") {
    it("must convert between basic types") {
      itMustConvertBetweenTypes[Long](123)
      itMustConvertBetweenTypes[Int](123)
      itMustConvertBetweenTypes[Short](123)
      itMustConvertBetweenTypes[Byte](123)
      itMustConvertBetweenTypes[Double](3232.11)
      itMustConvertBetweenTypes[Float](2.3f)
      itMustConvertBetweenTypes[Boolean](true)
    }

    def itMustConvertBetweenTypes[T <: AnyVal](value: T)(implicit m: Manifest[T]) {
      val format = ValueFormat.basicFormat[T]
      itMustFormatAndParse(format, value)
    }

    it("must fail to construct if no type specified") {
      intercept[IllegalArgumentException] {
        basicFormat[AnyVal]
      }
    }
  }

  describe("TextValueFormat") {
    it("must convert numbers") {
      itMustParseNumbers[Long](123)
      itMustParseNumbers[Int](123)
      itMustParseNumbers[Short](123)
      itMustParseNumbers[Byte](123)
    }

    it("must return None if unable to parse") {
      val format = new TextValueFormat[Int](NumberFormat.getIntegerInstance)
      format.toValue("blah") must be (None)
    }

    def itMustParseNumbers[T](value: T)(implicit m: Manifest[T]) {
      val format = new TextValueFormat[T](NumberFormat.getIntegerInstance)
      itMustFormatAndParse(format, value)
    }
  }

  describe("currencyValueFormat") {
    val format = currencyValueFormat

    it("must handle parse various number formats") {
      format.toValue("$1.00").get must be(1.0)
      format.toValue("$1").get must be(1.0)
      format.toValue("1.00").get must be(1.0)
      format.toValue("1").get must be(1.0)
      format.toValue("-1.00").get must be(-1.0)
      format.toValue("-1").get must be(-1.0)
      format.toValue("($1.00)").get must be(-1.0)
      format.toValue("($1)").get must be(-1.0)
      //do these later if desired
      format.toValue("(1.00)") must be(None)
      format.toValue("(1)") must be(None)
      format.toValue("-$1.00") must be(None)
      format.toValue("-$1") must be(None)
    }

    it("must format correctly") {
      format.toString(1234.2) must be ("1,234.20")
      format.toString(1234.22324) must be ("1,234.22")
    }
  }

  describe("currencyDisplayValueFormat") {
    val format = currencyDisplayValueFormat

    it("must handle parse various number formats") {
      format.toValue("$1.00").get must be(1.0)
      format.toValue("$1").get must be(1.0)
      format.toValue("1.00") must be(None)
      format.toValue("($1.00)").get must be(-1.0)
      format.toValue("($1)").get must be(-1.0)
      //do these later if desired
      format.toValue("(1.00)") must be(None)
      format.toValue("-$1.00") must be(None)
      format.toValue("-$1") must be(None)
    }

    it("must format correctly") {
      format.toString(1234.2) must be ("$1,234.20")
      format.toString(1234.22324) must be ("$1,234.22")
    }
  }

  describe("enumFormat") {
    object MyEnum extends Enumeration {
      val A = Value("A")
      val B = Value("B")
    }
    val format = enumFormat[MyEnum.Value](MyEnum)

    it("must handle formatting/parsing") {
      format.toString(MyEnum.A) must be ("A")
      format.toString(MyEnum.B) must be ("B")
      itMustFormatAndParse(format, MyEnum.A)
      itMustFormatAndParse(format, MyEnum.B)
      format.toValue("") must be (None)
    }
  }

  def itMustFormatAndParse[T](format: ValueFormat[T], value: T) {
    val string = format.toString(value)
    format.toValue(string).get must be (value)
  }
}