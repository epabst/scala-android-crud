package com.github.scala_android.crud

import android.app.Activity
import monitor.Logging

/**
 * Support for the different Crud Activity's.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/25/11
 * Time: 7:01 PM
 */

trait BaseCrudActivity[Q <: AnyRef,L <: AnyRef,R <: AnyRef,W <: AnyRef] extends Activity with PlatformTypes with Logging {
  def entityType: CrudEntityType[Q,L,R,W]

  def application: CrudApplication

  val crudContext = new CrudContext(this)

  override lazy val logTag = classOf[BaseCrudActivity[Q,L,R,W]].getName + "(" + entityType.entityName + ")"

  //available to be overridden for testing
  def openEntityPersistence(): EntityPersistence[Q,L,R,W] = entityType.openEntityPersistence(crudContext)

  def withPersistence[T](f: EntityPersistence[Q,L,R,W] => T): T = {
    val persistence = openEntityPersistence()
    try {
      f(persistence)
    } finally {
      persistence.close
    }
  }

  def addUndoableDelete(entityType: CrudEntityTypeRef, undoable: Undoable[ID]) {
    //todo implement
  }

  lazy val actionFactory = new ActivityUIActionFactory(this, application)
}
