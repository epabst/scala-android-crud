package com.github.scala_android.crud

/**
 * Persistence support for an entity.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/2/11
 * Time: 4:12 PM
 * @param Q the query criteria type
 * @param L the type of findAll (e.g. Cursor)
 * @param R the type to read from (e.g. Cursor)
 * @param W the type to write to (e.g. ContentValues)
 */

trait EntityPersistence[Q,L,R,W] extends PlatformTypes {
  def newCriteria: Q

  def findAll(criteria: Q): L

  def toIterator(list: L): Iterator[R]

  def findAsIterator(criteria: Q): Iterator[R] = toIterator(findAll(criteria))

  /** Find an entity by ID. */
  def find(id: ID): Option[R]

  /** Save a created or updated entity. */
  def save(id: Option[ID], writable: W): ID

  /**
   * Delete a list of entities by ID.
   * This should NOT delete child entities because that would make the "undo" functionality incomplete.
   * Instead, assume that the CrudEntityType will handle deleting all child entities explicitly.
   */
  def delete(ids: List[ID])

  def close()
}

trait CrudEntityPersistence[Q <: AnyRef,L <: AnyRef,R <: AnyRef,W <: AnyRef] extends EntityPersistence[Q,L,R,W] {
  def entityType: CrudEntityType[Q,L,R,W]

  def findAsIterator[T <: AnyRef](criteria: Q, instantiate: () => T): Iterator[T] =
    toIterator(findAll(criteria)).map(entity => {
      val result = instantiate()
      entityType.copyFields(entity, result)
      result
    })
}