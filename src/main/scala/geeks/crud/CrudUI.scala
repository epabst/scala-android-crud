package geeks.crud

/**
 * Crud API for one Entity type.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/22/11
 * Time: 10:41 PM
 * @param ID the ID type for the entity such as String or Long.
 * @param C the criteria for findAll
 */

trait CrudUI[ID,C] {
  /** Display a UI for a user to fill in data for creating an entity.  It should copy Unit into the UI to populate defaults. */
  def startCreateUI()

  /** Display a UI that shows matching entities (probably as a scrollable list). */
  def displayResults(criteria: C)

  /** Display a UI that shows an entity. */
  def display(id: ID)

  /** Display a UI to allow a user to edit an entity. */
  def startUpdateUI(id: ID)

  /** Display a UI to allow a user to proceed with deleting a list of entities. */
  def startDeleteUI(ids: List[ID])
}