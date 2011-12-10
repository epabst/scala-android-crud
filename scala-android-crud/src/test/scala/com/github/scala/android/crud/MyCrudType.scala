package com.github.scala.android.crud

import org.mockito.Mockito
import persistence.EntityType
import android.widget.ListAdapter

/**
 * A simple CrudType for testing.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/15/11
 * Time: 10:40 PM
 */
case class MyCrudType(override val entityType: EntityType, persistenceFactory: PersistenceFactory) extends PersistedCrudType(entityType, persistenceFactory) with StubCrudType {
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

  def setListAdapter(crudType: CrudType, findAllResult: Seq[AnyRef], contextItems: List[AnyRef], activity: CrudListActivity) {}

  def refreshAfterDataChanged(listAdapter: ListAdapter) {}
}

trait StubCrudType extends CrudType {
  override lazy val entityNameLayoutPrefix = "test"

  def listActivityClass = classOf[CrudListActivity]
  def activityClass = classOf[CrudActivity]
}
