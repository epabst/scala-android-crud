package com.github.scala_android.crud

import android.app.Activity
import android.net.Uri
import android.content.{Context, Intent}
import collection.JavaConversions
import com.github.triangle.PortableField
import com.github.triangle.ValueFormat.basicFormat
import common.PlatformTypes
import persistence.CrudPersistence

/**
 * A Factory for UIActions.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/25/11
 * Time: 5:33 PM
 */

trait UIActionFactory extends PlatformTypes {
  /**
   * The application.
   */
  def application: CrudApplication

  /**
   * Gets the current UI.  This can be helpful to get the current android Intent, etc.
   */
  def currentIntent: Intent

  /**
   * Gets the action to display a UI for a user to fill in data for creating an entity.
   * It should copy Unit into the UI using entityType.copy to populate defaults.
   */
  def startCreate(entityType: CrudType): UIAction[Unit]

  /**
   * Gets the action to display the list that matches the criteria copied from criteriaSource using entityType.copy.
   */
  def displayList(entityType: CrudType): UIAction[Option[EntityUriSegment]]

  /**
   * Gets the action to display the entity given the id.
   */
  def display(entityType: CrudType): UIAction[ID]

  /**
   * Gets the action to display a UI for a user to edit data for an entity given its id.
   */
  def startUpdate(entityType: CrudType): UIAction[ID]

  /**
   * Gets the action to display a UI to allow a user to proceed with deleting a list of entities given their ids.
   */
  def startDelete(entityType: CrudType): UIAction[ID]

  /** Puts the UI in place for the undo to be chosen. */
  def addUndoableDelete(entityType: CrudType, undoable: Undoable[ID])

  /**
   * Adapts an action from one type to another.
   * This is useful when dealing with childEntities and foreignKeys.
   */
  def adapt[T,F](uiAction: UIAction[F], f: T => F): UIAction[T]

  /** Pass-through in order to get the Context. */
  def withEntityPersistence[T,Q <: AnyRef,L <: AnyRef,R <: AnyRef,W <: AnyRef](entityType: CrudType, f: CrudPersistence => T): T
}


/**
 * Represents an action that a user can initiate.
 * It's equals/hashCode MUST be implemented in order to suppress the action that is already happening.
 */
trait UIAction[T] extends PlatformTypes {
  /** The icon to display for this action. */
  def icon: Option[ImgKey]

  /**The title to display for this action.
   * If the title is None, it won't be displayed in the context menu for an item.
   * If both title and icon are None,
   * then it won't be displayed in the main options menu, but can still be triggered as a default action.
   */
  def title: Option[SKey]

  def apply(value: T)
}

class ActivityUIActionFactory(currentActivity: BaseCrudActivity, val application: CrudApplication) extends UIActionFactory {
  private def thisFactory = this
  lazy val crudContext = currentActivity.crudContext
  def currentIntent = currentActivity.getIntent

  /**
   * Represents an action involving a crud entity.
   */
  abstract class CrudUIAction[T](val icon: Option[ImgKey], val title: Option[SKey], val entityType: CrudType) extends UIAction[T]

  private def toAction[T](icon: Option[ImgKey], title: Option[SKey], entityType: CrudType, intentGetter: T => Intent) =
    new CrudUIAction[T](icon, title, entityType) {
      def apply(value: T) {
        currentActivity.startActivity(intentGetter(value))
      }
    }

  def adapt[T,F](uiAction: UIAction[F], f: T => F): UIAction[T] = new UIAction[T] {
    val title = uiAction.title
    val icon = uiAction.icon

    def apply(value: T) = uiAction(f(value))
  }

  import ActivityUIActionFactory._
  import PortableField.toSome

  def startCreate(entityType: CrudType) =
    toAction(android.R.drawable.ic_menu_add, entityType.addItemString, entityType, _ =>
      getCreateIntent(entityType, currentIntent.getData, crudContext))

  def displayList(entityType: CrudType) =
    toAction[Option[EntityUriSegment]](None, entityType.listItemsString, entityType, value => getDisplayListIntent(entityType, currentIntent.getData, value, crudContext))

  def display(entityType: CrudType) =
    toAction[ID](None, None, entityType, id => getDisplayIntent(entityType, id, currentIntent.getData, crudContext))

  def startUpdate(entityType: CrudType) =
    toAction[ID](android.R.drawable.ic_menu_edit, entityType.editItemString, entityType, id => getUpdateIntent(entityType, id, currentIntent.getData, crudContext))

  def startDelete(entityTypeToDelete: CrudType) =
    new CrudUIAction[ID](android.R.drawable.ic_menu_delete, entityTypeToDelete.deleteItemString, entityTypeToDelete) {
      def apply(id: ID) {
        entityTypeToDelete.startDelete(id, thisFactory)
      }
    }

  def addUndoableDelete(entityType: CrudType, undoable: Undoable[ID]) {
    currentActivity.addUndoableDelete(entityType, undoable)
  }

  def withEntityPersistence[T, Q <: AnyRef, L <: AnyRef, R <: AnyRef, W <: AnyRef](entityType: CrudType, f: (CrudPersistence) => T) =
    entityType.withEntityPersistence(crudContext, f)
}

object ActivityUIActionFactory extends PlatformTypes {
  val CreateActionString = Intent.ACTION_INSERT
  val ListActionString = Intent.ACTION_PICK
  val DisplayActionString = Intent.ACTION_VIEW
  val UpdateActionString = Intent.ACTION_EDIT
  val DeleteActionString = Intent.ACTION_DELETE

  def getCreateIntent(entityType: CrudType, baseUri: Uri, crudContext: CrudContext): Intent =
    newIntent(CreateActionString, entityType.activityClass, entityType.entityName, detail = Nil, baseUri, crudContext)

  /**
   * Gets the intent for displaying a list of the entityType.
   * @param uriContext an optional EntityUriSegment to specify in the baseUri to provide any necessary context
   */
  def getDisplayListIntent(entityType: CrudType, baseUri: Uri, uriContext: Option[EntityUriSegment], crudContext: CrudContext): Intent =
    getDisplayListIntent(entityType, uriContext.map(_.specifyInUri(baseUri)).getOrElse(baseUri), crudContext)

  def getDisplayListIntent(entityType: CrudType, baseUri: Uri, crudContext: CrudContext): Intent =
    newIntent(ListActionString, entityType.listActivityClass, entityType.entityName, detail = Nil, baseUri, crudContext)

  def getDisplayIntent(entityType: CrudType, id: ID, baseUri: Uri, crudContext: CrudContext): Intent =
    newIntent(DisplayActionString, entityType.activityClass, entityType.entityName, detail = List(id.toString), baseUri, crudContext)

  def getUpdateIntent(entityType: CrudType, id: ID, baseUri: Uri, crudContext: CrudContext): Intent =
    newIntent(UpdateActionString, entityType.activityClass, entityType.entityName, detail = List(id.toString), baseUri, crudContext)

  private def newIntent(action: String, activityClass: Class[_ <: Activity],
                        entityName: String, detail: List[String], currentUri: Uri, crudContext: CrudContext) = {
    val newUri = EntityUriSegment(entityName, detail:_*).specifyInUri(currentUri)
    constructIntent(action, newUri, crudContext.context, activityClass)
  }

  def toUri(segments: String*): Uri = {
    segments.foldLeft(Uri.EMPTY)((uri, segment) => Uri.withAppendedPath(uri, segment))
  }

  //this is a workaround because Robolectric doesn't handle the full constructor
  def constructIntent(action: String, uri: Uri, context: Context, clazz: Class[_]) = {
    val intent = new Intent(action, uri)
    intent.setClass(context, clazz)
    intent
  }
}

case class EntityUriSegment(entityName: String, detail: String*) extends PlatformTypes {
  import JavaConversions._
  private val idFormat = basicFormat[ID]

  def specifyInUri(currentUri: Uri): Uri =
    replacePathSegments(currentUri, _.takeWhile(_ != entityName) ::: entityName :: detail.toList)

  def findId(currentUri: Uri): Option[ID] =
    currentUri.getPathSegments.toList.dropWhile(_ != entityName) match {
      case nameString :: idString :: x => idFormat.toValue(idString)
      case _ => None
    }

  private def replacePathSegments(uri: Uri, f: List[String] => List[String]): Uri = {
    val path = f(uri.getPathSegments.toList)
    ActivityUIActionFactory.toUri(path: _*)
  }
}