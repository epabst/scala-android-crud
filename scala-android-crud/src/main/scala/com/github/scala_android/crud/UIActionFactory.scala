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
   * It should copy Unit into the UI using entityConfig.copy to populate defaults.
   */
  def startCreate(entityType: CrudEntityType): CrudUIAction

  /**
   * Gets the action to display the list that matches the criteria copied from criteriaSource using entityConfig.copy.
   */
  def displayList(entityType: CrudEntityType, uriContext: Option[EntityUriSegment] = None): CrudUIAction

  /**
   * Gets the action to display the entity given the id.
   */
  def display(entityType: CrudEntityType, id: ID): CrudUIAction

  /**
   * Gets the action to display a UI for a user to edit data for an entity given its id.
   */
  def startUpdate(entityType: CrudEntityType, id: ID): CrudUIAction

  /**
   * Gets the action to display a UI to allow a user to proceed with deleting a list of entities given their ids.
   */
  def startDelete(entityType: CrudEntityType, ids: List[ID]): CrudUIAction
}


/**
 * Represents an action that a user can initiate.
 * It's equals/hashCode MUST be implemented in order to suppress the action that is already happening.
 */
trait UIAction {
  /** The icon to display for this action. */
  def icon: Option[Int]

  /** The title to display for this action.  If both title and icon are None,
   * then it won't be displayed as an option, but can still be triggered as a default action.
   */
  def title: Option[Int]

  def apply()
}

/**
 * Represents an action involving a crud entity.
 */
abstract class CrudUIAction(val icon: Option[Int], val title: Option[Int], val entityType: CrudEntityType) extends UIAction

class ActivityUIActionFactory(currentActivity: Activity) extends UIActionFactory {
  def currentIntent = currentActivity.getIntent

  private def toAction(icon: Option[Int], title: Option[Int], entityType: CrudEntityType, intent: Intent) =
    new CrudUIAction(icon, title, entityType) {
      def apply() {
        currentActivity.startActivity(intent)
      }
    }

  import ActivityUIActionFactory._
  import Field.toSome

  def startCreate(entityType: CrudEntityType) =
    toAction(android.R.drawable.ic_menu_add, entityType.addItemString, entityType,
      getCreateIntent(entityType, currentIntent.getData, currentActivity))

  def displayList(entityType: CrudEntityType, uriContext: Option[EntityUriSegment] = None) =
    toAction(None, entityType.listItemsString, entityType, getDisplayListIntent(entityType, currentIntent.getData, uriContext, currentActivity))

  def display(entityType: CrudEntityType, id: ID) =
    toAction(None, None, entityType, getDisplayIntent(entityType, id, currentIntent.getData, currentActivity))

  def startUpdate(entityType: CrudEntityType, id: ID) =
    toAction(android.R.drawable.ic_menu_edit, entityType.editItemString, entityType, getUpdateIntent(entityType, id, currentIntent.getData, currentActivity))

  def startDelete(entityType: CrudEntityType, ids: List[ID]) =
    toAction(android.R.drawable.ic_menu_delete, None, entityType, getDeleteIntent(entityType, ids, currentIntent.getData, currentActivity))
}

object ActivityUIActionFactory {
  type ID = Long
  val CreateActionString = Intent.ACTION_INSERT
  val ListActionString = Intent.ACTION_PICK
  val DisplayActionString = Intent.ACTION_VIEW
  val UpdateActionString = Intent.ACTION_EDIT
  val DeleteActionString = Intent.ACTION_DELETE

  def getCreateIntent(entityType: CrudEntityType, baseUri: Uri, context: Context): Intent =
    newIntent(CreateActionString, entityType.activityClass, entityType.entityName, detail = Nil, baseUri, context)

  /**
   * Gets the intent for displaying a list of the entityType.
   * @param uriContext an optional EntityUriSegment to specify in the baseUri to provide any necessary context
   */
  def getDisplayListIntent(entityType: CrudEntityType, baseUri: Uri, uriContext: Option[EntityUriSegment], context: Context): Intent =
    getDisplayListIntent(entityType, uriContext.map(_.specifyInUri(baseUri)).getOrElse(baseUri), context)

  def getDisplayListIntent(entityType: CrudEntityType, baseUri: Uri, context: Context): Intent =
    newIntent(ListActionString, entityType.listActivityClass, entityType.entityName, detail = Nil, baseUri, context)

  def getDisplayIntent(entityType: CrudEntityType, id: ID, baseUri: Uri, context: Context): Intent =
    newIntent(DisplayActionString, entityType.activityClass, entityType.entityName, detail = List(id.toString), baseUri, context)

  def getUpdateIntent(entityType: CrudEntityType, id: ID, baseUri: Uri, context: Context): Intent =
    newIntent(UpdateActionString, entityType.activityClass, entityType.entityName, detail = List(id.toString), baseUri, context)

  def getDeleteIntent(entityType: CrudEntityType, ids: List[ID], baseUri: Uri, context: Context): Intent =
    newIntent(DeleteActionString, entityType.activityClass, entityType.entityName, detail = List(ids.mkString(",")), baseUri, context)

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