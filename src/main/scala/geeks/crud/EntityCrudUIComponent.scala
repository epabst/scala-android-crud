package geeks.crud

/**
 * Crud API for one Entity type.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/22/11
 * Time: 10:41 PM
 */

trait EntityCrudUIComponent[L,R,W] {
  /** The ID type for the entity such as String or Long. */
  type ID

  trait EntityCrudUI {
    /** Display a UI for a user to fill in data for creating an entity.  It should copy Unit into the UI to populate defaults. */
    def startCreate()

    /** Display a UI that shows a (probably scrollable) list of entities. */
    def startList(listStream: L)

    /** Display a UI that shows an entity. */
    def startRead(readable: R)

    /** Display a UI to allow a user to edit an entity. */
    def startUpdate(id: ID)

    /** Display a UI to allow a user to proceed with deleting a list of entities. */
    def startDelete(ids: List[ID])
  }
}