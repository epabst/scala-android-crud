package com.github.scala_android.crud.persistence

import com.github.scala_android.crud.{CrudType, CrudContext}

/**
 * An EntityPersistence that is derived from another CrudType's persistence.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 9/20/11
 * Time: 9:30 PM
 */

abstract class DerivedEntityPersistence[T <: AnyRef](val delegate: CrudType, val crudContext: CrudContext)
        extends SeqEntityPersistence[T] {
  val delegatePersistence = delegate.openEntityPersistence(crudContext)

  override def close() { delegatePersistence.close() }
}
