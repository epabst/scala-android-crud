package com.github.scala.android.crud.persistence

import com.github.scala.android.crud.common.{ListenerHolder, Timing}
import com.github.scala.android.crud.common.PlatformTypes._
import com.github.scala.android.crud.common.UriPath

trait PersistenceListener {
  def onSave(id: ID)

  def onDelete(uri: UriPath)
}

/** Persistence support for an entity.
  * @author Eric Pabst (epabst@gmail.com)
  */

trait EntityPersistence extends Timing with ListenerHolder[PersistenceListener] {
  def toUri(id: ID): UriPath

  /** Finds one result for a given uri.  The UriPath should uniquely identify an entity.
    * @throws IllegalStateException if more than one entity matches the UriPath.
    */
  def find(uri: UriPath): Option[AnyRef] = {
    val results = findAll(uri)
    if (!results.isEmpty && !results.tail.isEmpty) throw new IllegalStateException("multiple results for " + uri + ": " + results.mkString(", "))
    results.headOption
  }

  def findAll(uri: UriPath): Seq[AnyRef]

  /** Should delegate to PersistenceFactory.newWritable. */
  def newWritable: AnyRef

  /** Save a created or updated entity. */
  final def save(idOption: Option[ID], writable: AnyRef): ID = {
    val id = doSave(idOption, writable)
    listeners.foreach(_.onSave(id))
    id
  }

  def doSave(id: Option[ID], writable: AnyRef): ID

  /** Delete a set of entities by uri.
    * This should NOT delete child entities because that would make the "undo" functionality incomplete.
    * Instead, assume that the CrudType will handle deleting all child entities explicitly.
    */
  final def delete(uri: UriPath) {
    doDelete(uri)
    listeners.foreach(_.onDelete(uri))
  }

  def doDelete(uri: UriPath)

  def close()
}
