package com.github.scala_android.crud

/**
 * Persistence support for an entity.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/2/11
 * Time: 4:12 PM
 */

trait EntityPersistence extends PlatformTypes {
  def newCriteria: AnyRef

  def findAll(criteria: AnyRef): AnyRef

  def toIterator(findAllResult: AnyRef): Iterator[AnyRef]

  def findAsIterator(criteria: AnyRef): Iterator[AnyRef] = toIterator(findAll(criteria))

  /** Find an entity by ID. */
  def find(id: ID): Option[AnyRef]

  /** Save a created or updated entity. */
  def save(id: Option[ID], writable: AnyRef): ID

  /**
   * Delete a list of entities by ID.
   * This should NOT delete child entities because that would make the "undo" functionality incomplete.
   * Instead, assume that the CrudEntityType will handle deleting all child entities explicitly.
   */
  def delete(ids: List[ID])

  def close()
}

trait CrudEntityPersistence extends EntityPersistence {
  def entityType: CrudEntityType

  def findAsIterator[T <: AnyRef](criteria: AnyRef, instantiateItem: () => T): Iterator[T] =
    toIterator(findAll(criteria)).map(entity => {
      val result = instantiateItem()
      entityType.copyFields(entity, result)
      result
    })
}