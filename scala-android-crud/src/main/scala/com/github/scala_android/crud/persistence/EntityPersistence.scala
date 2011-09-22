package com.github.scala_android.crud.persistence

import com.github.scala_android.crud.common.{ListenerHolder, Timing, PlatformTypes}
import android.net.Uri

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
  def toUri(id: ID): Uri

  def findAll(uri: Uri): Seq[AnyRef]

  /** Find an entity by ID. */
  def find(id: ID): Option[AnyRef] = findAll(toUri(id)).headOption

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
