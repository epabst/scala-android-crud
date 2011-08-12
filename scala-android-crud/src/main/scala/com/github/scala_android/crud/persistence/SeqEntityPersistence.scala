package com.github.scala_android.crud.persistence

import scala.collection.mutable
import IdPk._

/**
 * EntityPersistence for a simple generated Seq.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/11/11
 * Time: 5:05 PM
 */

trait SeqEntityPersistence[T <: AnyRef] extends CrudPersistence {
  def getId(entity: T): ID = idField(entity)

  def find(id: ID): Option[T] = {
    findAll(newCriteria).find(entity => id == getId(entity))
  }

  def findAll(criteria: AnyRef): Seq[T]

  def toIterator(seq: AnyRef) = seq.asInstanceOf[Seq[T]].toIterator

  def save(id: Option[ID], data: AnyRef): ID = throw new UnsupportedOperationException("write not supported")

  def delete(ids: Seq[ID]) { throw new UnsupportedOperationException("delete not supported") }

  def close() {}
}

trait ListBufferEntityPersistence[T <: AnyRef] extends SeqEntityPersistence[T] {
  val buffer = mutable.ListBuffer[T]()

  var nextId = 10000L

  def newCriteria = Unit

  def findAll(criteria: AnyRef) = buffer.toList

  override def save(id: Option[ID], item: AnyRef) = {
    val newId = id.getOrElse {
      nextId += 1
      nextId
    }
    buffer += idField.transformer(item.asInstanceOf[T])(Some(newId));
    newId
  }

  override def delete(ids: Seq[ID]) {
    ids.foreach(id => find(id).map(entity => buffer -= entity))
  }
}