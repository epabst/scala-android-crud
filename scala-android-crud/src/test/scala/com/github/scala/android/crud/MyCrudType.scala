package com.github.scala.android.crud

import org.mockito.Mockito
import persistence.EntityType
import android.widget.ListAdapter

/** A simple CrudType for testing.
  * @author Eric Pabst (epabst@gmail.com)
  */
case class MyCrudType(override val entityType: EntityType, override val persistenceFactory: PersistenceFactory)
  extends PersistedCrudType(entityType, persistenceFactory) with StubCrudType {

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
  override def newWritable = Map.empty[String,Any]

  def createEntityPersistence(entityType: EntityType, crudContext: CrudContext) = persistence

  def refreshAfterDataChanged(listAdapter: ListAdapter) {}
}

trait StubCrudType extends CrudType {
  override lazy val entityNameLayoutPrefix = "test"

  def listActivityClass = classOf[CrudListActivity]
  def activityClass = classOf[CrudActivity]
}
