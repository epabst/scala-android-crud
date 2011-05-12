package com.github.scala_android.crud

import scala.collection.mutable
import CursorField._

/**
 * EntityPersistence for a simple generated List.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/11/11
 * Time: 5:05 PM
 */

abstract class ListEntityPersistence[T <: AnyRef] extends CrudPersistence {
  def getId(entity: T): ID = persistedId(entity)

  def find(id: ID): Option[T] = {
    findAll(newCriteria).find(entity => id == getId(entity))
  }

  def findAll(criteria: AnyRef): List[T]

  def toIterator(list: AnyRef) = toIterator(list.asInstanceOf[List[T]])

  def toIterator(list: List[T]) = list.toIterator

  def save(id: Option[ID], data: AnyRef): ID = throw new UnsupportedOperationException("write not supported")

  def delete(ids: List[ID]) { throw new UnsupportedOperationException("delete not supported") }

  def close() {}
}

abstract class ListBufferEntityPersistence[T <: AnyRef] extends ListEntityPersistence[T] {
  val buffer = mutable.ListBuffer[T]()

  var nextId = 10000L

  def findAll(criteria: AnyRef) = buffer.toList

  override def save(id: Option[ID], item: AnyRef) = {
    val newId = id.getOrElse {
      nextId += 1
      nextId
    }
    persistedId.setter(item)(Some(newId));
    buffer += item.asInstanceOf[T]
    newId
  }

  override def delete(ids: List[ID]) {
    ids.foreach(id => find(id).map(entity => buffer -= entity))
  }
}