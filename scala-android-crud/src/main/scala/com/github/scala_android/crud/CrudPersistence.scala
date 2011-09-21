package com.github.scala_android.crud

import action.EntityUriSegment
import android.net.Uri
import persistence.{ListBufferEntityPersistence, SeqEntityPersistence, UriEntityPersistence}

/**
 * An EntityPersistence for a CrudType.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 9/21/11
 * Time: 6:39 AM
 */

trait CrudPersistence extends UriEntityPersistence {
  def entityType: CrudType

  def toUri(id: ID) = EntityUriSegment(entityType.entityName, id.toString).specifyInUri(Uri.EMPTY)

  def toCriteria(uri: Uri): AnyRef = entityType.transform(newCriteria, uri)

  def findAsIterator[T <: AnyRef](criteria: AnyRef, instantiateItem: => T): Iterator[T] =
    toIterator(findAll(criteria)).map(entity => {
      entityType.transform(instantiateItem, entity)
    })
}

trait SeqCrudPersistence[T <: AnyRef] extends SeqEntityPersistence[T] with CrudPersistence

trait ListBufferCrudPersistence[T <: AnyRef] extends SeqCrudPersistence[T] with ListBufferEntityPersistence[T]

/**
 * An EntityPersistence that is derived from another CrudType's persistence.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 9/20/11
 * Time: 9:30 PM
 */

abstract class DerivedCrudPersistence[T <: AnyRef](val delegate: CrudType, val crudContext: CrudContext)
        extends SeqCrudPersistence[T] {
  val delegatePersistence = delegate.openEntityPersistence(crudContext)

  override def close() { delegatePersistence.close() }
}
