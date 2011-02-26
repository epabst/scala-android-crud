package geeks.crud.android

import android.app.Activity

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
  def currentActivity: Activity

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
abstract class CrudUIAction(val entityType: CrudEntityType, val originalActivity: Activity) extends UIAction
