package com.github.scala.android.crud

import org.mockito.Mockito
import persistence.EntityType

/** A simple CrudType for testing.
  * @author Eric Pabst (epabst@gmail.com)
  */
case class MyCrudType(override val entityType: EntityType, override val persistenceFactory: PersistenceFactory)
  extends CrudType(entityType, persistenceFactory) with StubCrudType {

  def this(entityType: EntityType, persistence: CrudPersistence = Mockito.mock(classOf[CrudPersistence])) {
    this(entityType, new MyPersistenceFactory(persistence))
  }

  def this(persistenceFactory: PersistenceFactory) {
    this(new MyEntityType, persistenceFactory)
  }

  def this(persistence: CrudPersistence) {
    this(new MyEntityType, persistence)
  }
}

object MyCrudType extends MyCrudType(Mockito.mock(classOf[CrudPersistence]))

class MyPersistenceFactory(persistence: CrudPersistence) extends PersistenceFactory {
  def canSave = true

  override def newWritable = Map.empty[String,Any]

  def createEntityPersistence(entityType: EntityType, crudContext: CrudContext) = persistence
}

trait StubCrudType extends CrudType {
  override lazy val entityNameLayoutPrefix = "test"
}

object MyCrudApplication {
  def apply(crudTypes: CrudType*): CrudApplication = new CrudApplication {
    def name = "test app"

    override def primaryEntityType = crudTypes.head.entityType

    def allCrudTypes = crudTypes.toList

    def dataVersion = 1
  }
}