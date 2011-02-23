package geeks.crud

/**
 * Cake pattern component for EntityPersistence.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/9/11
 * Time: 6:49 AM
 * @param L the type of findAll (e.g. Cursor)
 * @param R the type to read from (e.g. Cursor)
 * @param W the type to write to (e.g. ContentValues)
 */

trait EntityPersistenceComponent[L,R,W] {
  type ID

  def persistence: EntityPersistence

  /**
   * Persistence support for an entity.
   * @author Eric Pabst (epabst@gmail.com)
   * Date: 2/2/11
   * Time: 4:12 PM
   */

  trait EntityPersistence {
    def findAll: L

    /** Find an entity by ID. */
    def find(id: ID): R

    def newWritable: W

    /** Save a created or updated entity. */
    def save(id: Option[ID], writable: W): ID

    /** Delete a list of entities by ID. */
    def delete(ids: List[ID])
  }
}