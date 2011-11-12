package com.github.scala.android.crud.persistence

import com.github.scala.android.crud.common.{ListenerHolder, Timing, PlatformTypes}
import com.github.scala.android.crud.action.UriPath

trait PersistenceListener extends PlatformTypes {
  def onSave(id: ID)

  def onDelete(uri: UriPath)
}

/**
 * Persistence support for an entity.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/2/11
 * Time: 4:12 PM
 */

trait EntityPersistence extends PlatformTypes with Timing with ListenerHolder[PersistenceListener] {
  def toUri(id: ID): UriPath

  /**
    * Finds one result for a given uri.  The UriPath should uniquely identify an entity.
    * @throws IllegalStateException if more than one entity matches the UriPath.
    */
  def find(uri: UriPath): Option[AnyRef] = {
    val results = findAll(uri)
    if (!results.isEmpty && !results.tail.isEmpty) throw new IllegalStateException("multiple results for " + uri + ": " + results.mkString(", "))
    results.headOption
  }

  def findAll(uri: UriPath): Seq[AnyRef]

  /** Save a created or updated entity. */
  final def save(idOption: Option[ID], writable: AnyRef): ID = {
    val id = doSave(idOption, writable)
    listeners.foreach(_.onSave(id))
    id
  }

  protected def doSave(id: Option[ID], writable: AnyRef): ID

  /**
   * Delete a set of entities by uri.
   * This should NOT delete child entities because that would make the "undo" functionality incomplete.
   * Instead, assume that the CrudType will handle deleting all child entities explicitly.
   */
  final def delete(uri: UriPath) {
    doDelete(uri)
    listeners.foreach(_.onDelete(uri))
  }

  protected def doDelete(uri: UriPath)

  def close()
}
