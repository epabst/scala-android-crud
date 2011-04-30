package com.github.triangle

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.Spec
import com.github.triangle.Field._
import scala.collection.mutable


/**
 * A behavior specification for {@link FieldList}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 4/21/11
 * Time: 1:39 AM
 */

@RunWith(classOf[JUnitRunner])
class FieldListSpec extends Spec with ShouldMatchers {
  describe("copyFields") {
    it("should copy values that apply") {
      val countField = Field[Int](default(10) + mapAccess("count"))
      val priceField = Field[Double](mapAccess("price"))
      val fields = FieldList(countField, priceField)
      val map = mutable.Map[String, Any]()

      //copy where only one field has an accessor
      fields.copyFields(Unit, map)
      map.contains("count") should be (true)
      countField(map) should be (10)
      map.contains("price") should be (false)

      fields.copyFields(mutable.Map("price" -> 300.00), map)
      map.contains("price") should be (true)
      priceField(map) should be (300.00)
      //should have been overwritten because the Map didn't have it
      countField.findValue(map) should be (None)
    }

    it("should return the Fields that were not copied") {
      val countField = Field[Int](default(10) + mapAccess("count"))
      val priceField = Field[Double](mapAccess("price"))
      val fields = FieldList(countField, priceField)
      val map = mutable.Map[String, Any]()

      //copy where only one field has an accessor
      val resultOfCopy: FieldList = fields.copyFields(Unit, map)
      map.contains("count") should be (true)
      countField(map) should be (10)
      map.contains("price") should be (false)
      resultOfCopy.toList should be (List(priceField))

      resultOfCopy.copyFields(mutable.Map("price" -> 300.00), map).size should be (0)
      map.contains("price") should be (true)
      priceField(map) should be (300.00)
      countField(map) should be (10)
    }
  }
}