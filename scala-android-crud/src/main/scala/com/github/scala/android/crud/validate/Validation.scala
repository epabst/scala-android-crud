package com.github.scala.android.crud.validate

import com.github.triangle.Transformer
import scala.{PartialFunction, AnyRef}

case class ValidationResult(numInvalid: Int) {
  def +(isValid: Boolean): ValidationResult = if (isValid) this else ValidationResult(numInvalid + 1)
}

object ValidationResult {
  /** The result for valid data.  It is capitalized so it can be used in case statements. */
  val Valid: ValidationResult = ValidationResult(0)
}

/** A PortableField for validating data.  It transforms a ValidationResult using a value.
  * @author Eric Pabst (epabst@gmail.com)
  */
class Validation[T](isValid: Option[T] => Boolean) extends Transformer[T] {
  def transformer[S <: AnyRef]: PartialFunction[S,Option[T] => S] = {
    case result: ValidationResult => value => (result + isValid(value)).asInstanceOf[S]
  }
}

object Validation {
  def apply[T](isValid: Option[T] => Boolean): Validation[T] = new Validation[T](isValid)

  /** A Validation that requires that the value be defined.
    * It does allow the value to be an empty string, empty list, etc.
    * Example: <pre>field... + required</pre>
    */
  def required[T]: Validation[T] = Validation(_.isDefined)

  /** A Validation that requires that the value be defined and not one of the given values.
    * Example: <pre>field... + requiredAndNot("")</pre>
    */
  def requiredAndNot[T](invalidValues: T*): Validation[T] =
    Validation(value => value.isDefined && !invalidValues.contains(value.get))

  /** A Validation that requires that the value not be empty (after trimming). */
  def requiredString: Validation[String] = Validation(_.map(s => s.trim != "").getOrElse(false))
}
