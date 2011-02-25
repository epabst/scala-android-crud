package geeks.crud

/**
 * An entity configuration that provides all custom information needed to
 * implement CRUD on the entity.  This shouldn't depend on the platform (e.g. android).
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/23/11
 * Time: 3:24 PM
 * @param ID the ID type
 * @param TT a translatable text identifier
 * @param LT a layout configuration
 */

trait CrudEntityType[TT] {
  def entityName: String

  def addItemString: TT
  def editItemString: TT
  def cancelItemString: TT
}

trait CrudEntityConfig[_ID,TT,LT] extends CrudEntityType[TT] {
  //this makes it available for subtypes to use to make it clear that it's an ID
  type ID = _ID

  def fields: List[CopyableField]

  def headerLayout: LT
  def listLayout: LT
  def rowLayout: LT
  def entryLayout: LT

  /**
   * Gets the actions that a user can perform from a list of the entities.
   * May be overridden to modify the list of actions.
   */
  def getListActions(actionFactory: UIActionFactory[ID,TT]): List[UIAction] =
    List(actionFactory.displayList(this), actionFactory.startCreate(this))

  /**
   * Gets the actions that a user can perform from a specific entity instance.
   * The first one is the one that will be used when the item is clicked on.
   * May be overridden to modify the list of actions.
   */
  def getEntityActions(actionFactory: UIActionFactory[ID,TT], id: ID): List[UIAction] =
    List(actionFactory.display(this, id), actionFactory.startUpdate(this, id), actionFactory.startDelete(this, List(id)))

  def copyFields(from: AnyRef, to: AnyRef) {
    fields.foreach(_.copy(from, to))
  }
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
trait CrudUIAction[TT] extends UIAction {
  def entityType: CrudEntityType[TT]
}

trait UIActionFactory[_ID,TT] {
  type ID = _ID

  /**
   * Gets the current UI.  This can be helpful to get the current android Intent, etc.
   */
  def currentUI: AnyRef

  /**
   * Gets the action to display a UI for a user to fill in data for creating an entity.
   * It should copy Unit into the UI using entityConfig.copy to populate defaults.
   */
  def startCreate(entityType: CrudEntityType[TT]): CrudUIAction[TT]

  /**
   * Gets the action to display the list that matches the criteria copied from criteriaSource using entityConfig.copy.
   */
  def displayList(entityType: CrudEntityType[TT], criteriaSource: AnyRef = Unit): CrudUIAction[TT]

  /**
   * Gets the action to display the entity given the id.
   */
  def display(entityType: CrudEntityType[TT], id: ID): CrudUIAction[TT]

  /**
   * Gets the action to display a UI for a user to edit data for an entity given its id.
   */
  def startUpdate(entityType: CrudEntityType[TT], id: ID): CrudUIAction[TT]

  /**
   * Gets the action to display a UI to allow a user to proceed with deleting a list of entities given their ids.
   */
  def startDelete(entityType: CrudEntityType[TT], ids: List[ID]): CrudUIAction[TT]
}
