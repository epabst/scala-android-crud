package com.github.scala.android.crud

import action.UriPath
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

  def findAll[T <: AnyRef](uri: UriPath, instantiateItem: => T): Seq[T] =
    findAll(uri).map(entityType.transform(instantiateItem, _))
}

trait SeqCrudPersistence[T <: AnyRef] extends SeqEntityPersistence[T] with CrudPersistence

trait ListBufferCrudPersistence[T <: AnyRef] extends SeqCrudPersistence[T] with ListBufferEntityPersistence[T]

/**
 * An EntityPersistence that is derived from other CrudType persistence(s).
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 9/20/11
 * Time: 9:30 PM
 */

abstract class DerivedCrudPersistence[T <: AnyRef](val crudContext: CrudContext, delegates: CrudType*)
        extends SeqCrudPersistence[T] {
  val delegatePersistenceMap: Map[CrudType,CrudPersistence] =
    Map.empty ++ delegates.toList.map(delegate => delegate -> delegate.openEntityPersistence(crudContext))
  def delegatePersistence: CrudPersistence = delegatePersistenceMap(delegates.head)

  override def close() {
    delegatePersistenceMap.values.foreach(_.close())
    super.close()
  }
}
