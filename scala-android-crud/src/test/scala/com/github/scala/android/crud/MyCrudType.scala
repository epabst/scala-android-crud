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
object MyCrudType extends MyCrudType(Mockito.mock(classOf[CrudPersistence]))

case class MyCrudType(persistenceFactory: PersistenceFactory) extends PersistedCrudType(persistenceFactory) with MyEntityType with StubEntityType {
  def this(persistence: CrudPersistence = Mockito.mock(classOf[CrudPersistence])) {
    this(new MyPersistenceFactory(persistence))
  }

  override def entityName = "MyCrudType"
}

class MyPersistenceFactory(persistence: CrudPersistence) extends PersistenceFactory {
  override def newWritable = Map.empty[String,Any]

  def createEntityPersistence(entityType: EntityType, crudContext: CrudContext) = persistence

  def setListAdapter(crudType: CrudType, findAllResult: Seq[AnyRef], contextItems: List[AnyRef], activity: CrudListActivity) {}

  def refreshAfterDataChanged(listAdapter: ListAdapter) {}
}

trait StubEntityType extends CrudType {
  override lazy val entityNameLayoutPrefix = "test"

  def listActivityClass = classOf[CrudListActivity]
  def activityClass = classOf[CrudActivity]
}
