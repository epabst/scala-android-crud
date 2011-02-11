package geeks.crud

/**
 * Cake pattern component for EntityPersistence.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/9/11
 * Time: 6:49 AM
 */

trait EntityPersistenceComponent[T] {
  def persistence: EntityPersistence[T]

  /**
   * Persistence support for an entity.
   * @author Eric Pabst (epabst@gmail.com)
   * Date: 2/2/11
   * Time: 4:12 PM
   */

  trait EntityPersistence[T] {
    def create: T

    def save(entity: T)
  }
}