package com.github.scala_android.crud

/**
 * EntityPersistence for a simple generated List.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/11/11
 * Time: 5:05 PM
 */

abstract class ListEntityPersistence[T <: AnyRef,Q <: AnyRef] extends EntityPersistence[Q,List[T],T,T] {
  def getId(entity: T): ID

  def find(id: ID): Option[T] = {
    findAll(newCriteria).find(entity => id == getId(entity))
  }

  def toIterator(list: List[T]) = list.toIterator

  def save(id: Option[ID], data: T) = throw new UnsupportedOperationException("write not supported")

  def delete(ids: List[ID]) = throw new UnsupportedOperationException("delete not supported")

  def close() {}
}