package com.github.scala_android.crud

import action.Action
import android.app.Activity
import common.{Timing, PlatformTypes}
import com.github.triangle.Logging
import action.UriPath
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

  lazy val contentProviderAuthority = Option(application.getClass.getPackage).getOrElse(getClass.getPackage).toString
  lazy val defaultContentUri = UriPath("content://" + contentProviderAuthority) / entityType.entityName

  lazy val currentUriPath: UriPath = {
    Option(getIntent).map(i => Option(i.getData).map(UriPath(_)).getOrElse {
      // If no data was given in the intent (because we were started
      // as a MAIN activity), then use our default content provider.
      getIntent.setData(Action.toUri(defaultContentUri))
      defaultContentUri
    }).getOrElse(defaultContentUri)
  }

  lazy val currentAction: String = getIntent.getAction

  def uriWithId(id: ID): UriPath = currentUriPath.specify(entityType.entityName, id.toString)

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
    action.invoke(currentUriPath, this)
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
