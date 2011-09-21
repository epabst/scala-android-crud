package com.github.scala_android.crud.persistence

import scala.collection.mutable
import IdPk._

/**
 * EntityPersistence for a simple generated Seq.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/11/11
 * Time: 5:05 PM
 */

trait SeqEntityPersistence[T <: AnyRef] extends UriEntityPersistence {
  def find(id: ID): Option[T] = {
    val criteria = toCriteria(toUri(id))
    findAll(criteria).find(entity => id == idField(entity))
  }

  def findAll(criteria: AnyRef): Seq[T]

  def toIterator(seq: AnyRef) = seq.asInstanceOf[Seq[T]].toIterator

  protected def doSave(id: Option[ID], data: AnyRef): ID = throw new UnsupportedOperationException("write not supported")

  protected def doDelete(ids: Seq[ID]) { throw new UnsupportedOperationException("delete not supported") }

  def close() {}
}

trait ListBufferEntityPersistence[T <: AnyRef] extends SeqEntityPersistence[T] {
  val buffer = mutable.ListBuffer[T]()

  var nextId = 10000L

  def newCriteria = Unit

  def findAll(criteria: AnyRef) = buffer.toList

  override protected def doSave(id: Option[ID], item: AnyRef) = {
    val newId = id.getOrElse {
      nextId += 1
      nextId
    }
    buffer += idField.transformer(item.asInstanceOf[T])(Some(newId));
    newId
  }

  override protected def doDelete(ids: Seq[ID]) {
    ids.foreach(id => find(id).map(entity => buffer -= entity))
  }
}