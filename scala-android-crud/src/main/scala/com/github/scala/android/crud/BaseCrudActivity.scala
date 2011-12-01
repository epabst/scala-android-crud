package com.github.scala.android.crud

import action._
import com.github.triangle.Logging
import android.view.MenuItem
import android.content.Intent
import common.{Common, Timing, PlatformTypes}
import PlatformTypes._

/**
 * Support for the different Crud Activity's.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/25/11
 * Time: 7:01 PM
 */

trait BaseCrudActivity extends ActivityWithVars with OptionsMenuActivity with Logging with Timing {
  def entityType: CrudType

  def application: CrudApplication

  lazy val contentProviderAuthority = Option(application.packageName).getOrElse(getClass.getPackage.getName)
  lazy val defaultContentUri = UriPath("content://" + contentProviderAuthority) / entityType.entityName

  override def setIntent(newIntent: Intent) {
    info("Current Intent: " + newIntent)
    super.setIntent(newIntent)
  }

  def currentUriPath: UriPath = {
    Option(getIntent).map(intent => Option(intent.getData).map(UriPath(_)).getOrElse {
      // If no data was given in the intent (because we were started
      // as a MAIN activity), then use our default content provider.
      intent.setData(Action.toUri(defaultContentUri))
      defaultContentUri
    }).getOrElse(defaultContentUri)
  }

  lazy val currentAction: String = getIntent.getAction

  def uriWithId(id: ID): UriPath = currentUriPath.specify(entityType.entityName, id.toString)

  val crudContext = new CrudContext(this, application)

  protected lazy val logTag = Common.tryToEvaluate(entityType.entityName).getOrElse(Common.logTag)

  protected def normalActions: List[Action]

  /** A ContextVar that holds an undoable Action if present. */
  private object LastUndoable extends ContextVar[Undoable]

  def allowUndo(undoable: Undoable) {
    // Finish any prior undoable first.  This could be re-implemented to support a stack of undoable operations.
    LastUndoable.clear(this).foreach(_.closeAction.foreach(_.invoke(currentUriPath, this)))
    // Remember the new undoable operation
    LastUndoable.set(this, undoable)
    optionsMenu = generateOptionsMenu
  }

  protected def applicableActions: List[Action] = LastUndoable.get(this).map(_.undoAction).toList ++ normalActions

  protected def generateOptionsMenu: List[Action] =
    applicableActions.filter(action => action.title.isDefined || action.icon.isDefined)

  def initialOptionsMenu = generateOptionsMenu

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    val actions = initialOptionsMenu
    actions.find(_.actionId == item.getItemId) match {
      case Some(action) =>
        action.invoke(currentUriPath, this)
        true
      case None => super.onOptionsItemSelected(item)
    }
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

  override def toString = getClass.getSimpleName + "@" + System.identityHashCode(this)
}
