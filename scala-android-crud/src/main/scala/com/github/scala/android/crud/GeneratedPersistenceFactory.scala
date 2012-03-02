package com.github.scala.android.crud

import common.CachedFunction
import persistence.EntityType

trait GeneratedPersistenceFactory[T <: AnyRef] extends PersistenceFactory {
  def canSave = false

  def newWritable: T = throw new UnsupportedOperationException("not supported")

  def createEntityPersistence(entityType: EntityType, crudContext: CrudContext): SeqCrudPersistence[T]
}

object GeneratedPersistenceFactory {
  def apply[T <: AnyRef](persistenceFunction: EntityType => SeqCrudPersistence[T]): GeneratedPersistenceFactory[T] = new GeneratedPersistenceFactory[T] {
    private val cachedPersistenceFunction = CachedFunction(persistenceFunction)

    def createEntityPersistence(entityType: EntityType, crudContext: CrudContext) = cachedPersistenceFunction(entityType)
  }
}
