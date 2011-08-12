package com.github.scala_android.crud.persistence

import com.github.triangle.FieldList
import com.github.scala_android.crud.common.{Timing, PlatformTypes}

/**
 * Persistence support for an entity.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/2/11
 * Time: 4:12 PM
 */

trait EntityPersistence extends PlatformTypes with Timing {
  def newCriteria: AnyRef

  def findAll(criteria: AnyRef): AnyRef

  def toIterator(findAllResult: AnyRef): Iterator[AnyRef]

  def findAsIterator(criteria: AnyRef): Iterator[AnyRef] = toIterator(findAll(criteria))

  /** Find an entity by ID. */
  def find(id: ID): Option[AnyRef]

  /** Save a created or updated entity. */
  def save(id: Option[ID], writable: AnyRef): ID

  /**
   * Delete a seq of entities by ID.
   * This should NOT delete child entities because that would make the "undo" functionality incomplete.
   * Instead, assume that the CrudType will handle deleting all child entities explicitly.
   */
  def delete(ids: Seq[ID])

  def close()
}

trait CrudPersistence extends EntityPersistence {
  def entityType: FieldList

  def findAsIterator[T <: AnyRef](criteria: AnyRef, instantiateItem: () => T): Iterator[T] =
    toIterator(findAll(criteria)).map(entity => {
      val result = instantiateItem()
      entityType.copy(entity, result)
      result
    })
}