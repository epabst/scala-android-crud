package com.github.scala.android.crud

import action._
import android.view.MenuItem
import android.content.Intent
import common.{UriPath, Common, Timing, PlatformTypes}
import PlatformTypes._
import com.github.scala.android.crud.view.AndroidConversions._
import android.os.Bundle
import com.github.triangle.{FieldList, PortableField, Logging}
import view.{ViewField, OnClickOperationSetter}

/** Support for the different Crud Activity's.
  * @author Eric Pabst (epabst@gmail.com)
  */

trait BaseCrudActivity extends ActivityWithVars with OptionsMenuActivity with Logging with Timing {
  def crudApplication: CrudApplication = super.getApplication.asInstanceOf[CrudAndroidApplication].application

  lazy val crudType: CrudType = crudApplication.allCrudTypes.find(crudType => Some(crudType.entityName) == currentUriPath.lastEntityNameOption).getOrElse {
    throw new IllegalStateException("No valid entityName in " + currentUriPath)
  }

  final def entityType = crudType.entityType

  override def setIntent(newIntent: Intent) {
    info("Current Intent: " + newIntent)
    super.setIntent(newIntent)
  }

  def currentUriPath: UriPath = {
    val defaultContentUri = crudApplication.defaultContentUri
    Option(getIntent).map(intent => Option(intent.getData).map(toUriPath(_)).getOrElse {
      // If no data was given in the intent (because we were started
      // as a MAIN activity), then use our default content provider.
      intent.setData(defaultContentUri)
      defaultContentUri
    }).getOrElse(defaultContentUri)
  }

  lazy val currentAction: String = getIntent.getAction

  def uriWithId(id: ID): UriPath = currentUriPath.specify(entityType.entityName, id.toString)

  lazy val crudContext = new CrudContext(this, crudApplication)

  def contextItems = List(currentUriPath, crudContext, PortableField.UseDefaults)

  def contextItemsWithoutUseDefaults = List(currentUriPath, crudContext)

  protected lazy val logTag = Common.tryToEvaluate(entityType.entityName).getOrElse(Common.logTag)

  /** This should be a lazy val in subclasses. */
  protected def normalActions: Seq[Action]

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

  protected lazy val normalOperationSetters: FieldList = {
    val setters = normalActions.filter(_.command.viewRef.isDefined).map(action =>
      ViewField.viewId[Nothing](action.command.viewRef.get, OnClickOperationSetter(_ => action.operation)))
    FieldList.toFieldList(setters)
  }

  protected def bindNormalActionsToViews() {
    normalOperationSetters.defaultValue.copyTo(this, contextItems)
  }

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

  override def onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    // This is after the super call so that outState can be overridden if needed.
    crudContext.onSaveState(this, outState)
  }

  override def onRestoreInstanceState(savedInstanceState: Bundle) {
    // This is before the super call to be the opposite order as onSaveInstanceState.
    crudContext.onRestoreState(this, savedInstanceState)
    super.onRestoreInstanceState(savedInstanceState)
  }

  override def onResume() {
    trace("onResume")
    crudContext.onClearState(this, stayActive = true)
    super.onResume()
  }

  override def onDestroy() {
    crudContext.vars.onDestroyContext()
    super.onDestroy()
  }

  //available to be overridden for testing
  def openEntityPersistence(): CrudPersistence = crudType.openEntityPersistence(crudContext)

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
