package com.github.triangle

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.Spec
import com.github.triangle.PortableField._


/**
 * A behavior specification for {@link FieldTuple}.
 * @autho5 E0ic Pabst (epabst@gmail.com)
 * Date: 4/20/11
 * Time: 1:39 PM
 */

@RunWith(classOf[JUnitRunner])
class FieldTupleSpec extends Spec with MustMatchers {
  val intField = default[Int](10)
  val stringField = default[String]("Hello")
  val doubleField = default[Double](11.0)

  it("must extract the field values") {
    val tuple = new FieldTuple3(intField, stringField, doubleField) {
      //use it to match a single AnyRef
      Unit match {
        case Values(integer, string, double) => {
          integer must be (Some(10))
          string must be (Some("Hello"))
          double must be (Some(11.0))
        }
      }
      //use it to match a List of AnyRefs
      List(Unit) match {
        case Values(integer, string, double) => {
          integer must be (Some(10))
          string must be (Some("Hello"))
          double must be (Some(11.0))
        }
      }
    }
    //use it external from the tuple itself
    val tuple.Values(integer, string, double) = Unit
    integer must be (Some(10))
    string must be (Some("Hello"))
    double must be (Some(11.0))
  }

  it("must work for tuple size of 1") {
    val tuple = new FieldTuple1(stringField)
    Unit match { case tuple.Values(Some("Hello")) => "ok"; case _ => fail() }
  }

  it("must work for tuple size of 2") {
    val tuple = new FieldTuple2(intField, stringField)
    Unit match { case tuple.Values(Some(10), Some("Hello")) => "ok"; case _ => fail() }
  }

  it("must work for tuple size of 3") {
    val tuple = new FieldTuple3(intField, stringField, doubleField)
    Unit match { case tuple.Values(Some(10), Some("Hello"), Some(11.0)) => "ok"; case _ => fail() }
  }

  it("must work for tuple size of 4") {
    val tuple = new FieldTuple4(intField, stringField, doubleField, stringField)
    Unit match { case tuple.Values(Some(10), Some("Hello"), Some(11.0), Some("Hello")) => "ok"; case _ => fail() }
  }

  it("must work for tuple size of 5") {
    val tuple = new FieldTuple5(intField, stringField, doubleField, stringField, intField)
    Unit match { case tuple.Values(Some(10), Some("Hello"), Some(11.0), Some("Hello"), Some(10)) => "ok"; case _ => fail() }
  }

  it("must work for tuple size of 6") {
    val tuple = new FieldTuple6(intField, stringField, doubleField, stringField, intField, doubleField)
    Unit match { case tuple.Values(Some(10), Some("Hello"), Some(11.0), Some("Hello"), Some(10), Some(11.0)) => "ok"; case _ => fail() }
  }

  it("must work for tuple size of 7") {
    val tuple = new FieldTuple7(intField, stringField, doubleField, stringField, intField, doubleField, intField)
    Unit match { case tuple.Values(Some(10), Some("Hello"), Some(11.0), Some("Hello"), Some(10), Some(11.0), Some(10)) => "ok"; case _ => fail() }
  }

  it("must work with a CalculatedField") {
    val field = new FieldTuple2(intField, identityField[String]) with CalculatedField[String] {
      def calculate = {
        case Values(Some(integer), Some(string)) => Some(string + integer)
      }
    }
    field.getterFromItem(List[AnyRef](Unit, "A String")) must be (Some("A String10"))
  }
}