package com.github.triangle

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
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
class ValueFormatSpec extends Spec with ShouldMatchers {
  describe("BasicValueFormat") {
    it("should convert between basic types") {
      itShouldConvertBetweenTypes[Long](123)
      itShouldConvertBetweenTypes[Int](123)
      itShouldConvertBetweenTypes[Short](123)
      itShouldConvertBetweenTypes[Byte](123)
      itShouldConvertBetweenTypes[Double](3232.11)
      itShouldConvertBetweenTypes[Float](2.3f)
      itShouldConvertBetweenTypes[Boolean](true)
    }

    def itShouldConvertBetweenTypes[T <: AnyVal](value: T)(implicit m: Manifest[T]) {
      val format = new BasicValueFormat[T]
      val string = format.toString(value)
      format.toValue(string).get should be (value)
    }
  }

  describe("TextValueFormat") {
    it("should convert numbers") {
      itShouldParseNumbers[Long](123)
      itShouldParseNumbers[Int](123)
      itShouldParseNumbers[Short](123)
      itShouldParseNumbers[Byte](123)
    }

    it("should return None if unable to parse") {
      val format = new TextValueFormat[Int](NumberFormat.getIntegerInstance)
      format.toValue("blah") should be (None)
    }

    def itShouldParseNumbers[T](value: T)(implicit m: Manifest[T]) {
      val format = new TextValueFormat[T](NumberFormat.getIntegerInstance)
      val string = format.toString(value)
      format.toValue(string).get should be (value)
    }
  }

  describe("currencyValueFormat") {
    val format = currencyValueFormat

    it("should handle parse various number formats") {
      format.toValue("$1.00").get should be(1.0)
      format.toValue("$1").get should be(1.0)
      format.toValue("1.00").get should be(1.0)
      format.toValue("1").get should be(1.0)
      format.toValue("-1.00").get should be(-1.0)
      format.toValue("-1").get should be(-1.0)
      format.toValue("($1.00)").get should be(-1.0)
      format.toValue("($1)").get should be(-1.0)
      //do these later if desired
      format.toValue("(1.00)") should be(None)
      format.toValue("(1)") should be(None)
      format.toValue("-$1.00") should be(None)
      format.toValue("-$1") should be(None)
    }

    it("should format correctly") {
      format.toString(1234.2) should be ("1,234.20")
      format.toString(1234.22324) should be ("1,234.22")
    }
  }

  describe("currencyDisplayValueFormat") {
    val format = currencyDisplayValueFormat

    it("should handle parse various number formats") {
      format.toValue("$1.00").get should be(1.0)
      format.toValue("$1").get should be(1.0)
      format.toValue("1.00") should be(None)
      format.toValue("($1.00)").get should be(-1.0)
      format.toValue("($1)").get should be(-1.0)
      //do these later if desired
      format.toValue("(1.00)") should be(None)
      format.toValue("-$1.00") should be(None)
      format.toValue("-$1") should be(None)
    }

    it("should format correctly") {
      format.toString(1234.2) should be ("$1,234.20")
      format.toString(1234.22324) should be ("$1,234.22")
    }
  }
}