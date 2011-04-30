package com.github.triangle

import scala.collection._

/**
 * A trait that has a list of Fields.  The only requirement is that <code>fields</code> be defined.
 * It has helpful methods that can operate on them.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 4/21/11
 * Time: 1:36 AM
 */
trait FieldList extends Traversable[BaseField] {
  import FieldList.toFieldList

  protected def fields: Traversable[BaseField]

  def foreach[U](f: (BaseField) => U) { fields.foreach(f) }

  def copyFields(from: AnyRef, to: AnyRef): FieldList = {
    toFieldList(fields.flatMap(f => if (f.copy(from, to)) None else Some(f)))
  }

  def fieldAccessFlatMap[B](f: PartialFunction[BaseField, Traversable[B]]): List[B] =
    fields.flatMap(_.flatMap(f)).toList
}

object FieldList {
  def apply(_fields: BaseField*): FieldList = toFieldList(_fields)

  implicit def toFieldList(list: Traversable[BaseField]): FieldList = new FieldList { def fields = list }
}
