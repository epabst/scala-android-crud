package com.github.scala_android.crud

import action.{Action, EntityUriSegment}
import android.app.Activity
import common.{Timing, PlatformTypes, Logging}
import persistence.CrudPersistence
import android.net.Uri
import android.view.{MenuItem, Menu}

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

  protected def applicableActions: List[Action]

  protected def optionsMenuActions: List[Action] =
    applicableActions.filter(action => action.title.isDefined || action.icon.isDefined)

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    val actions = optionsMenuActions
    for (action <- actions) {
      val index = actions.indexOf(action)
      val menuItem = action.title.map(menu.add(0, index, index, _)).getOrElse(menu.add(0, index, index, ""))
      action.icon.map(icon => menuItem.setIcon(icon))
    }
    true
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    val actions = optionsMenuActions
    val action = actions(item.getItemId)
    action.invoke(currentUri, this)
    true
  }

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
