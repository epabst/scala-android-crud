package geeks.crud

import _root_.android.content.ContentValues
import _root_.android.database.Cursor

/**
 * Cake pattern component for EntityPersistence.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/9/11
 * Time: 6:49 AM
 */

trait EntityPersistenceComponent {
  type ID

  def persistence: EntityPersistence

  /**
   * Persistence support for an entity.
   * @author Eric Pabst (epabst@gmail.com)
   * Date: 2/2/11
   * Time: 4:12 PM
   */

  trait EntityPersistence {
    def findAll: Cursor

    def find(id: ID): Cursor

    def save(id: Option[ID], contentValues: ContentValues): ID
  }
}