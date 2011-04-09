package com.github.scala_android.crud

import android.content.ContentValues

/**
 * EntityPersistence for a simple generated List.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/11/11
 * Time: 5:05 PM
 */

abstract class ListEntityPersistence[T <: AnyRef,Q <: AnyRef](entityType: CrudEntityType[Q,List[T],T,ContentValues])
        extends EntityPersistence[Q,List[T],T,ContentValues] {
  def getId(entity: T): ID

  def find(id: ID): Option[T] = {
    findAll(newCriteria).find(entity => id == getId(entity))
  }

  def save(id: Option[ID], contentValues: ContentValues) =
    throw new UnsupportedOperationException("write not suppoted")

  def delete(ids: List[ID]) = throw new UnsupportedOperationException("delete not supported")

  def close() {}
}