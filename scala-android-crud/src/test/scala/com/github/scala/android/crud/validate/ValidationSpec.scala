package com.github.scala.android.crud.validate

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalatest.matchers.MustMatchers
import org.scalatest.Spec
import Validation._

/** A behavior specification for [[com.github.scala.android.crud.validate.Validation]].
  * @author Eric Pabst (epabst@gmail.com)
  */
@RunWith(classOf[JUnitRunner])
class ValidationSpec extends Spec with MustMatchers {
  describe("required") {
    val transformer = required[Int].transformer[ValidationResult]

    it("must detect an empty value") {
      transformer(ValidationResult.Valid)(None) must be (ValidationResult(1))
    }

    it("must accept a defined value") {
      transformer(ValidationResult.Valid)(Some(0)) must be (ValidationResult.Valid)
    }
  }

  describe("requiredAndNot") {
    val transformer = requiredAndNot[Int](1, 3).transformer[ValidationResult]

    it("must detect an empty value") {
      transformer(ValidationResult.Valid)(None) must be (ValidationResult(1))
    }

    it("must detect a matching (non-empty) value and consider it invalid") {
      transformer(ValidationResult.Valid)(Some(3)) must be (ValidationResult(1))
    }

    it("must detect a non-matching (non-empty) value and consider it valid") {
      transformer(ValidationResult.Valid)(Some(2)) must be (ValidationResult.Valid)
    }
  }

  describe("requiredString") {
    val transformer = requiredString.transformer[ValidationResult]

    it("must detect an empty value") {
      transformer(ValidationResult.Valid)(None) must be (ValidationResult(1))
    }

    it("must consider an empty string as invalid") {
      transformer(ValidationResult.Valid)(Some("")) must be (ValidationResult(1))
    }

    it("must consider an string with just whitespace as invalid") {
      transformer(ValidationResult.Valid)(Some(" \t\r\n ")) must be (ValidationResult(1))
    }

    it("must consider a non-empty string as valid") {
      transformer(ValidationResult.Valid)(Some("hello")) must be (ValidationResult.Valid)
    }
  }
}
