package com.github.scala.android.crud

import com.github.triangle.PortableField.identityField
import android.widget.ListAdapter
import common.{CachedFunction, UriPath}
import persistence.EntityType
import com.github.triangle.Field
import view.EntityAdapter

trait GeneratedPersistenceFactory[T <: AnyRef] extends PersistenceFactory {
  def newWritable: T = throw new UnsupportedOperationException("not supported")

  def createEntityPersistence(entityType: EntityType, crudContext: CrudContext): SeqCrudPersistence[T]

  def setListAdapter(crudType: CrudType, findAllResult: Seq[AnyRef], contextItems: List[AnyRef], activity: CrudListActivity) {
    activity.setListAdapter(new EntityAdapter(crudType.entityType, findAllResult, crudType.rowLayout, contextItems, activity.getLayoutInflater))
  }

  def refreshAfterDataChanged(listAdapter: ListAdapter) {}
}

object GeneratedPersistenceFactory {
  def apply[T <: AnyRef](persistenceFunction: EntityType => SeqCrudPersistence[T]): GeneratedPersistenceFactory[T] = new GeneratedPersistenceFactory[T] {
    private val cachedPersistenceFunction = CachedFunction(persistenceFunction)

    def createEntityPersistence(entityType: EntityType, crudContext: CrudContext) = cachedPersistenceFunction(entityType)
  }
}

abstract class GeneratedCrudType[T <: AnyRef](entityType: EntityType, persistenceFactory: GeneratedPersistenceFactory[T])
  extends CrudType(entityType, persistenceFactory) {

  override def getListActions(application: CrudApplication) = super.getReadOnlyListActions(application)

  override def getEntityActions(application: CrudApplication) = super.getReadOnlyEntityActions(application)
}

object GeneratedCrudType {
  object CrudContextField extends Field(identityField[CrudContext])
  object UriField extends Field(identityField[UriPath])
}