package com.github.triangle

/**
 * A PortableField that is calculated using the fields in a FieldTuple.
 * Instantiate along with a FieldTuple of the right -arity such as FieldTuple2.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 5/20/11
 * Time: 9:50 AM
 */
trait CalculatedField[T] extends PortableField[T] with NoTransformer[T] with FieldTuple {
  /**
   * Calculate the value.  The <code>Values</code> extractor should be used to get the other field values:
   * <pre>
   *   case Values(value1, value2) => Some(value1 * value2)
   * </pre>
   */
  def calculate: PartialFunction[List[AnyRef], Option[T]]

  //inherited
  override def getterFromItem = calculate

  /** Delegates to getterFromItem to make that the common entry point. */
  def getter = {
    case from if getterFromItem.isDefinedAt(List(from)) => getterFromItem(List(from))
  }

  override def toString = "FieldTuple(" + productIterator.mkString(",") + ") with CalculatedField"
}
