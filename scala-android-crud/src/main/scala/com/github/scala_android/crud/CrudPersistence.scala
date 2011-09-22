package com.github.scala_android.crud

import android.net.Uri
import persistence.{ListBufferEntityPersistence, SeqEntityPersistence, EntityPersistence}

/**
 * An EntityPersistence for a CrudType.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 9/21/11
 * Time: 6:39 AM
 */

trait CrudPersistence extends EntityPersistence {
  def entityType: CrudType

  def toUri(id: ID) = entityType.toUri(id)

  def findAll[T <: AnyRef](uri: Uri, instantiateItem: => T): Seq[T] =
    findAll(uri).map(entityType.transform(instantiateItem, _))
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
