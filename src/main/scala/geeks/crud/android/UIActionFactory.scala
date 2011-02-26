package geeks.crud.android

import android.app.Activity
import android.net.Uri
import android.content.{Context, Intent}
import collection.JavaConversions

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
  def displayList(entityType: CrudEntityType, criteriaSource: AnyRef = Unit): CrudUIAction

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
  def apply()
}

/**
 * Represents an action involving a crud entity.
 */
abstract class CrudUIAction(val entityType: CrudEntityType) extends UIAction

class ActivityUIActionFactory(currentActivity: Activity) extends UIActionFactory {
  def currentIntent = currentActivity.getIntent

  private def toAction(entityType: CrudEntityType, intent: Intent) = new CrudUIAction(entityType) {
    def apply() {
      currentActivity.startActivity(intent)
    }
  }

  import ActivityUIActionFactory._

  def startCreate(entityType: CrudEntityType) =
    toAction(entityType, getCreateIntent(entityType, currentIntent, currentActivity))

  def displayList(entityType: CrudEntityType, criteriaSource: AnyRef) =
    toAction(entityType, getDisplayListIntent(entityType, criteriaSource, currentIntent, currentActivity))

  def display(entityType: CrudEntityType, id: ID) =
    toAction(entityType, getDisplayIntent(entityType, id, currentIntent, currentActivity))

  def startUpdate(entityType: CrudEntityType, id: ID) =
    toAction(entityType, getUpdateIntent(entityType, id, currentIntent, currentActivity))

  def startDelete(entityType: CrudEntityType, ids: List[ID]) =
    toAction(entityType, getDeleteIntent(entityType, ids, currentIntent, currentActivity))
}

object ActivityUIActionFactory {
  type ID = Long
  val CreateActionString = Intent.ACTION_INSERT
  val ListActionString = Intent.ACTION_PICK
  val DisplayActionString = Intent.ACTION_VIEW
  val UpdateActionString = Intent.ACTION_EDIT
  val DeleteActionString = Intent.ACTION_DELETE

  def getCreateIntent(entityType: CrudEntityType, currentIntent: Intent, context: Context): Intent = {
    val currentUri = currentIntent.getData
    val index = currentUri.getPathSegments.indexOf(entityType.entityName)
    val uri = if (index >= 0) {
      import JavaConversions._
      toUri(currentUri.getPathSegments.subList(0, index + 1).toList: _*)
    } else {
      Uri.withAppendedPath(currentUri, entityType.entityName)
    }
    newIntent(CreateActionString, uri, context, entityType.activityClass)
  }

  def getDisplayListIntent(entityType: CrudEntityType, criteriaSource: AnyRef, currentIntent: Intent, context: Context): Intent = {
    //todo fix
    currentIntent
  }

  def getDisplayIntent(entityType: CrudEntityType, id: ID, currentIntent: Intent, context: Context): Intent = {
    //todo fix
    currentIntent
  }

  def getUpdateIntent(entityType: CrudEntityType, id: ID, currentIntent: Intent, context: Context): Intent = {
    //todo fix
    currentIntent
  }

  def getDeleteIntent(entityType: CrudEntityType, ids: List[ID], currentIntent: Intent, context: Context): Intent = {
    //todo fix
    currentIntent
  }

  def toUri(segments: String*): Uri = {
    segments.foldLeft(Uri.EMPTY)((uri, segment) => Uri.withAppendedPath(uri, segment))
  }

  //this is a workaround because Robolectric doesn't handle the full constructor
  private def newIntent(action: String, uri: Uri, context: Context, clazz: Class[_]) = {
    val intent = new Intent(action, uri)
    intent.setClass(context, clazz)
    intent
  }
}
