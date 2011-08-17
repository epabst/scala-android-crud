package com.github.scala_android.crud.persistence

import com.github.triangle.FieldList
import com.github.scala_android.crud.common.{ListenerHolder, Timing, PlatformTypes}

trait PersistenceListener extends PlatformTypes {
  def onSave(id: ID)

  def onDelete(ids: Seq[ID])
}

/**
 * Persistence support for an entity.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/2/11
 * Time: 4:12 PM
 */

trait EntityPersistence extends PlatformTypes with Timing with ListenerHolder[PersistenceListener] {
  def newCriteria: AnyRef

  def findAll(criteria: AnyRef): AnyRef

  def toIterator(findAllResult: AnyRef): Iterator[AnyRef]

  def findAsIterator(criteria: AnyRef): Iterator[AnyRef] = toIterator(findAll(criteria))

  /** Find an entity by ID. */
  def find(id: ID): Option[AnyRef]

  /** Save a created or updated entity. */
  final def save(idOption: Option[ID], writable: AnyRef): ID = {
    val id = doSave(idOption, writable)
    listeners.foreach(_.onSave(id))
    id
  }

  protected def doSave(id: Option[ID], writable: AnyRef): ID

  /**
   * Delete a seq of entities by ID.
   * This should NOT delete child entities because that would make the "undo" functionality incomplete.
   * Instead, assume that the CrudType will handle deleting all child entities explicitly.
   */
  final def delete(ids: Seq[ID]) {
    doDelete(ids)
    listeners.foreach(_.onDelete(ids))
  }

  protected def doDelete(ids: Seq[ID])

  def close()
}

trait CrudPersistence extends EntityPersistence {
  def entityType: FieldList

  def findAsIterator[T <: AnyRef](criteria: AnyRef, instantiateItem: () => T): Iterator[T] =
    toIterator(findAll(criteria)).map(entity => {
      entityType.transform(instantiateItem(), entity)
    })
}