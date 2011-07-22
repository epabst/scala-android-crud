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

  protected def fields: Traversable[BaseField]

  def foreach[U](f: (BaseField) => U) { fields.foreach(f) }

  def copyFields(from: AnyRef, to: AnyRef) {
    fields.foreach(_.copy(from, to))
  }

  def copyFieldsFromItem(fromItems: List[AnyRef], to: AnyRef) {
    fields.foreach(_.copyFromItem(fromItems, to))
  }

  def transform[S <: AnyRef](initial: S, data: AnyRef): S = {
    fields.foldLeft(initial)((subject, field) => field.transform(subject, data))
  }

  def transformWithItem[S <: AnyRef](initial: S, dataItems: List[AnyRef]): S = {
    fields.foldLeft(initial)((subject, field) => field.transformWithItem(subject, dataItems))
  }

  def fieldFlatMap[B](f: PartialFunction[BaseField, Traversable[B]]): List[B] =
    fields.flatMap(_.flatMap(f)).toList
}

object FieldList {
  def apply(_fields: BaseField*): FieldList = toFieldList(_fields)

  implicit def toFieldList(list: Traversable[BaseField]): FieldList = new FieldList { def fields = list }
}
