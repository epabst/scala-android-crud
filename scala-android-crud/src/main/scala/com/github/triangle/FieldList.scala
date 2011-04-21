package com.github.triangle

import scala.collection._

/**
 * A trait that has a list of Fields.  The only requirement is that <code>fields</code> be defined.
 * It has helpful methods that can operate on them.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 4/21/11
 * Time: 1:36 AM
 */
trait FieldList extends Traversable[CopyableField] {
  protected def fields: Traversable[CopyableField]

  def foreach[U](f: (CopyableField) => U) { fields.foreach(f) }

  def copyFields(from: AnyRef, to: AnyRef) {
    fields.foreach(_.copy(from, to))
  }

  def fieldAccessFlatMap[B](f: (PartialFieldAccess[_]) => Traversable[B]): List[B] =
    fields.map(_.asInstanceOf[Field[_]].fieldAccesses).flatMap(_.flatMap(f)).toList
}

object FieldList {
  def apply(_fields: CopyableField*): FieldList = new FieldList { val fields = List(_fields:_*) }
}
