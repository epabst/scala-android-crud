package com.github.scala_android.crud

import android.app.Activity
import android.net.Uri
import android.content.{Context, Intent}
import collection.JavaConversions
import com.github.triangle.{BasicValueFormat, Field}

/**
 * A Factory for UIActions.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/25/11
 * Time: 5:33 PM
 */

trait UIActionFactory {
  type ID = Long

  /**
   * Gets the current UI.  This can be helpful to get the current android Intent, etc.
   */
  def currentIntent: Intent

  /**
   * Gets the action to display a UI for a user to fill in data for creating an entity.
   * It should copy Unit into the UI using entityType.copy to populate defaults.
   */
  def startCreate(entityType: CrudEntityTypeRef): UIAction[Unit]

  /**
   * Gets the action to display the list that matches the criteria copied from criteriaSource using entityType.copy.
   */
  def displayList(entityType: CrudEntityTypeRef): UIAction[Option[EntityUriSegment]]

  /**
   * Gets the action to display the entity given the id.
   */
  def display(entityType: CrudEntityTypeRef): UIAction[ID]

  /**
   * Gets the action to display a UI for a user to edit data for an entity given its id.
   */
  def startUpdate(entityType: CrudEntityTypeRef): UIAction[ID]

  /**
   * Gets the action to display a UI to allow a user to proceed with deleting a list of entities given their ids.
   */
  def startDelete(entityType: CrudEntityTypeRef): UIAction[ID]
}


/**
 * Represents an action that a user can initiate.
 * It's equals/hashCode MUST be implemented in order to suppress the action that is already happening.
 */
trait UIAction[T] {
  /** The icon to display for this action. */
  def icon: Option[Int]

  /**The title to display for this action.
   * If the title is None, it won't be displayed in the context menu for an item.
   * If both title and icon are None,
   * then it won't be displayed in the main options menu, but can still be triggered as a default action.
   */
  def title: Option[Int]

  def apply(value: T)
}

/**
 * Represents an action involving a crud entity.
 */
abstract class CrudUIAction[T](val icon: Option[Int], val title: Option[Int], val entityType: CrudEntityTypeRef) extends UIAction[T]

class ActivityUIActionFactory(currentActivity: Activity) extends UIActionFactory {
  def currentIntent = currentActivity.getIntent

  private def toAction[T](icon: Option[Int], title: Option[Int], entityType: CrudEntityTypeRef, intentGetter: T => Intent) =
    new CrudUIAction[T](icon, title, entityType) {
      def apply(value: T) {
        currentActivity.startActivity(intentGetter(value))
      }
    }

  import ActivityUIActionFactory._
  import Field.toSome

  def startCreate(entityType: CrudEntityTypeRef) =
    toAction(android.R.drawable.ic_menu_add, entityType.addItemString, entityType, _ =>
      getCreateIntent(entityType, currentIntent.getData, currentActivity))

  def displayList(entityType: CrudEntityTypeRef) =
    toAction[Option[EntityUriSegment]](None, entityType.listItemsString, entityType, value => getDisplayListIntent(entityType, currentIntent.getData, value, currentActivity))

  def display(entityType: CrudEntityTypeRef) =
    toAction[ID](None, None, entityType, id => getDisplayIntent(entityType, id, currentIntent.getData, currentActivity))

  def startUpdate(entityType: CrudEntityTypeRef) =
    toAction[ID](android.R.drawable.ic_menu_edit, entityType.editItemString, entityType, id => getUpdateIntent(entityType, id, currentIntent.getData, currentActivity))

  def startDelete(entityType: CrudEntityTypeRef) =
    toAction[ID](android.R.drawable.ic_menu_delete, None, entityType, id => getDeleteIntent(entityType, id, currentIntent.getData, currentActivity))
}

object ActivityUIActionFactory {
  type ID = Long
  val CreateActionString = Intent.ACTION_INSERT
  val ListActionString = Intent.ACTION_PICK
  val DisplayActionString = Intent.ACTION_VIEW
  val UpdateActionString = Intent.ACTION_EDIT
  val DeleteActionString = Intent.ACTION_DELETE

  def adapt[T,F](uiAction: UIAction[F], f: T => F): UIAction[T] = new UIAction[T] {
    val title = uiAction.title
    val icon = uiAction.icon

    def apply(value: T) = uiAction(f(value))
  }

  def getCreateIntent(entityType: CrudEntityTypeRef, baseUri: Uri, context: Context): Intent =
    newIntent(CreateActionString, entityType.activityClass, entityType.entityName, detail = Nil, baseUri, context)

  /**
   * Gets the intent for displaying a list of the entityType.
   * @param uriContext an optional EntityUriSegment to specify in the baseUri to provide any necessary context
   */
  def getDisplayListIntent(entityType: CrudEntityTypeRef, baseUri: Uri, uriContext: Option[EntityUriSegment], context: Context): Intent =
    getDisplayListIntent(entityType, uriContext.map(_.specifyInUri(baseUri)).getOrElse(baseUri), context)

  def getDisplayListIntent(entityType: CrudEntityTypeRef, baseUri: Uri, context: Context): Intent =
    newIntent(ListActionString, entityType.listActivityClass, entityType.entityName, detail = Nil, baseUri, context)

  def getDisplayIntent(entityType: CrudEntityTypeRef, id: ID, baseUri: Uri, context: Context): Intent =
    newIntent(DisplayActionString, entityType.activityClass, entityType.entityName, detail = List(id.toString), baseUri, context)

  def getUpdateIntent(entityType: CrudEntityTypeRef, id: ID, baseUri: Uri, context: Context): Intent =
    newIntent(UpdateActionString, entityType.activityClass, entityType.entityName, detail = List(id.toString), baseUri, context)

  def getDeleteIntent(entityType: CrudEntityTypeRef, id: ID, baseUri: Uri, context: Context): Intent =
    //todo don't start a new intent - if on the individual entity, go back until you're not.  otherwise, stayhere
    newIntent(DeleteActionString, entityType.listActivityClass, entityType.entityName, detail = List(id.toString), baseUri, context)

  private def newIntent(action: String, activityClass: Class[_ <: Activity],
                        entityName: String, detail: List[String], currentUri: Uri, context: Context) = {
    val newUri = EntityUriSegment(entityName, detail:_*).specifyInUri(currentUri)
    constructIntent(action, newUri, context, activityClass)
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

case class EntityUriSegment(entityName: String, detail: String*) {
  import JavaConversions._
  private val longFormat = new BasicValueFormat[Long]()

  def specifyInUri(currentUri: Uri): Uri =
    replacePathSegments(currentUri, _.takeWhile(_ != entityName) ::: entityName :: detail.toList)

  def findId(currentUri: Uri): Option[Long] =
    currentUri.getPathSegments.toList.dropWhile(_ != entityName) match {
      case nameString :: idString :: x => longFormat.toValue(idString)
      case _ => None
    }

  private def replacePathSegments(uri: Uri, f: List[String] => List[String]): Uri = {
    val path = f(uri.getPathSegments.toList)
    ActivityUIActionFactory.toUri(path: _*)
  }
}