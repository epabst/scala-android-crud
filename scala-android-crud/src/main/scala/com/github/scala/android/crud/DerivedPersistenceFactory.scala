package com.github.scala.android.crud

import common.UriPath
import persistence.EntityType

/** A CrudPersistence that is derived from related CrudType persistence(s).
  * @author Eric Pabst (epabst@gmail.com)
  * @see DerivedPersistenceFactory
  */
abstract class DerivedCrudPersistence[T <: AnyRef](val crudContext: CrudContext, delegates: CrudType*)
        extends SeqCrudPersistence[T] {
  val delegatePersistenceMap: Map[EntityType,CrudPersistence] =
    delegates.map(delegate => delegate -> delegate.openEntityPersistence(crudContext)).toMap

  override def close() {
    delegatePersistenceMap.values.foreach(_.close())
    super.close()
  }
}

/** A PersistenceFactory that is derived from related CrudType persistence(s).
  * @author Eric Pabst (epabst@gmail.com)
  */
abstract class DerivedPersistenceFactory[T <: AnyRef](delegates: CrudType*) extends GeneratedPersistenceFactory[T] { self =>
  def findAll(entityType: EntityType, uri: UriPath, delegatePersistenceMap: Map[EntityType,CrudPersistence]): Seq[T]

  def createEntityPersistence(_entityType: EntityType, crudContext: CrudContext) = {
    new DerivedCrudPersistence[T](crudContext, delegates: _*) {
      def entityType = _entityType

      def findAll(uri: UriPath): Seq[T] = self.findAll(_entityType, uri, delegatePersistenceMap)
    }
  }
}
