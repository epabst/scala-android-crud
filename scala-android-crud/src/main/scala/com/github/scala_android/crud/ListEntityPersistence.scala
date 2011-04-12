package com.github.scala_android.crud

import scala.collection.mutable
import CursorFieldAccess._

/**
 * EntityPersistence for a simple generated List.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/11/11
 * Time: 5:05 PM
 */

abstract class ListEntityPersistence[T <: AnyRef,Q <: AnyRef] extends EntityPersistence[Q,List[T],T,T] {
  def getId(entity: T) = persistedId.partialGet(entity).get

  def find(id: ID): Option[T] = {
    findAll(newCriteria).find(entity => id == getId(entity))
  }

  def toIterator(list: List[T]) = list.toIterator

  def save(id: Option[ID], data: T): ID = throw new UnsupportedOperationException("write not supported")

  def delete(ids: List[ID]) { throw new UnsupportedOperationException("delete not supported") }

  def close() {}
}

abstract class ListBufferEntityPersistence[T <: AnyRef,Q <: AnyRef] extends ListEntityPersistence[T,Q] {
  val buffer = mutable.ListBuffer[T]()

  var nextId = 10000L

  def findAll(criteria: Q) = buffer.toList

  override def save(id: Option[ID], item: T) = {
    val newId = id.getOrElse {
      nextId += 1
      nextId
    }
    persistedId.partialSet(item, newId);
    buffer += item
    newId
  }

  override def delete(ids: List[ID]) = {
    ids.foreach(id => find(id).map(entity => buffer -= entity))
  }
}