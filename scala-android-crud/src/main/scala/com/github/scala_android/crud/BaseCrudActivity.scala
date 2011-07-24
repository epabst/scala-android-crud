package com.github.scala_android.crud

import android.app.Activity
import monitor.Logging

/**
 * Support for the different Crud Activity's.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/25/11
 * Time: 7:01 PM
 */

trait BaseCrudActivity extends Activity with PlatformTypes with Logging with Timing {
  def entityType: CrudType

  def application: CrudApplication

  val crudContext = new CrudContext(this, application)

  override lazy val logTag = classOf[BaseCrudActivity].getName + "(" + entityType.entityName + ")"

  //available to be overridden for testing
  def openEntityPersistence(): CrudPersistence = entityType.openEntityPersistence(crudContext)

  def withPersistence[T](f: CrudPersistence => T): T = {
    val persistence = openEntityPersistence()
    try {
      f(persistence)
    } finally {
      persistence.close
    }
  }

  def addUndoableDelete(entityType: CrudType, undoable: Undoable[ID]) {
    //todo implement
  }

  lazy val actionFactory = new ActivityUIActionFactory(this, crudContext.application)
}
