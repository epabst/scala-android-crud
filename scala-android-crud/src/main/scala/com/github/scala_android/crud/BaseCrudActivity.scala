package com.github.scala_android.crud

import action.EntityUriSegment
import android.app.Activity
import common.{Timing, PlatformTypes, Logging}
import persistence.CrudPersistence
import android.net.Uri

/**
 * Support for the different Crud Activity's.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/25/11
 * Time: 7:01 PM
 */

trait BaseCrudActivity extends Activity with PlatformTypes with Logging with Timing {
  def entityType: CrudType

  def application: CrudApplication

  def currentUri: Uri = getIntent.getData

  def uriWithId(id: ID): Uri = EntityUriSegment(entityType.entityName, id.toString).specifyInUri(currentUri)

  val crudContext = new CrudContext(this, application)

  override lazy val logTag = classOf[BaseCrudActivity].getName + "(" + entityType.entityName + ")"

  //available to be overridden for testing
  def openEntityPersistence(): CrudPersistence = entityType.openEntityPersistence(crudContext)

  def withPersistence[T](f: CrudPersistence => T): T = {
    val persistence = openEntityPersistence()
    try {
      f(persistence)
    } finally {
      persistence.close()
    }
  }

  def addUndoableDelete(entityType: CrudType, undoable: Undoable[ID]) {
    //todo implement
  }

  override def toString = getClass.getSimpleName + "@" + System.identityHashCode(this)
}
