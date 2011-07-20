package com.github.triangle

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.Spec
import com.github.triangle.PortableField._
import scala.collection._


/**
 * A behavior specification for {@link FieldList}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 4/21/11
 * Time: 1:39 AM
 */

@RunWith(classOf[JUnitRunner])
class FieldListSpec extends Spec with MustMatchers {
  describe("copyFields") {
    it("must copy values that apply") {
      val countField = default[Int](10) + mapField("count")
      val priceField = mapField[Double]("price")
      val fields = FieldList(countField, priceField)
      val map = mutable.Map[String, Any]()

      //copy where only one field has an accessor
      fields.copyFields(Unit, map)
      map.contains("count") must be (true)
      countField(map) must be (10)
      map.contains("price") must be (false)

      fields.copyFields(mutable.Map("price" -> 300.00), map)
      map.contains("price") must be (true)
      priceField(map) must be (300.00)
      //must have been overwritten because the Map didn't have it
      countField.getter(map) must be (None)
    }

    it("must copy values that apply from list of items") {
      //intentionally put default before mapField
      val countField = default[Int](10) + mapField[Int]("count")
      val priceField = mapField[Double]("price")
      val fields = FieldList(countField, priceField)
      val map = mutable.Map[String, Any]()

      val itemList = List(new Object, Unit, Map("count" -> 11, "price" -> 300.0))
      fields.copyFieldsFromItem(itemList, map)
      //should use the default since first in the item list
      map.get("count") must be (Some(10))
      map.get("price") must be (Some(300.00))
    }

    it("must return the Fields that were not copied") {
      val countField = default[Int](10) + mapField("count")
      val priceField = mapField[Double]("price")
      val fields = FieldList(countField, priceField)
      val map = mutable.Map[String, Any]()

      //copy where only one field has an accessor
      val resultOfCopy: FieldList = fields.copyFields(Unit, map)
      map.contains("count") must be (true)
      countField(map) must be (10)
      map.contains("price") must be (false)
      resultOfCopy.toList must be (List(priceField))

      resultOfCopy.copyFields(mutable.Map("price" -> 300.00), map).size must be (0)
      map.contains("price") must be (true)
      priceField(map) must be (300.00)
      countField(map) must be (10)
    }

    it("must transform using each Field") {
      val countField = mapField[Int]("count")
      val priceField = mapField[Double]("price")
      val fields = FieldList(countField, priceField)
      val result = fields.transform(initial = immutable.Map.empty[String,Any],
                                    data = immutable.Map[String,Any]("ignored" -> "bar", "price" -> 100.0, "count" -> 10))
      result must be (immutable.Map[String,Any]("count" -> 10, "price" -> 100.0))
    }

    it("must transform using the first applicable item for each Field") {
      val countField = default(12) + mapField[Int]("count")
      val priceField = mapField[Double]("price")
      val fields = FieldList(countField, priceField)
      val result = fields.transformWithItem(initial = immutable.Map.empty[String,Any],
                                            dataItems = List(Unit, immutable.Map[String, Any]("ignored" -> "bar", "price" -> 100.0)))
      result must be (immutable.Map[String,Any]("count" -> 12, "price" -> 100.0))
    }
  }
}