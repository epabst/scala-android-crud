package com.github.scala.android.crud

import action._
import com.github.triangle.Logging
import android.view.MenuItem
import android.content.Intent
import common.{UriPath, Common, Timing, PlatformTypes}
import PlatformTypes._
import RichIntent._

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
    Option(getIntent).map(intent => Option(intent.getData).map(toUriPath(_)).getOrElse {
      // If no data was given in the intent (because we were started
      // as a MAIN activity), then use our default content provider.
      intent.setData(Operation.toUri(defaultContentUri))
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
    LastUndoable.clear(this).foreach(_.closeOperation.foreach(_.invoke(currentUriPath, this)))
    // Remember the new undoable operation
    LastUndoable.set(this, undoable)
    optionsMenuCommands = generateOptionsMenu.map(_.command)
  }

  protected def applicableActions: List[Action] = LastUndoable.get(this).map(_.undoAction).toList ++ normalActions

  protected def generateOptionsMenu: List[Action] =
    applicableActions.filter(action => action.command.title.isDefined || action.command.icon.isDefined)

  def initialOptionsMenuCommands = generateOptionsMenu.map(_.command)

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    val actions = generateOptionsMenu
    actions.find(_.commandId == item.getItemId) match {
      case Some(action) =>
        action.invoke(currentUriPath, this)
        if (LastUndoable.get(this).exists(_.undoAction.commandId == item.getItemId)) {
          LastUndoable.clear(this)
          optionsMenuCommands = generateOptionsMenu.map(_.command)
        }
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
