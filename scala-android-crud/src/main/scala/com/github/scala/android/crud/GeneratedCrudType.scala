package com.github.scala.android.crud

import android.view.{ViewGroup, View}
import com.github.triangle.PortableField.identityField
import android.app.ListActivity
import android.widget.{ListAdapter, BaseAdapter}
import common.PlatformTypes._
import common.{CachedFunction, UriPath}
import persistence.EntityType
import com.github.triangle.Field

trait GeneratedPersistenceFactory[T <: AnyRef] extends PersistenceFactory {
  def newWritable: T = throw new UnsupportedOperationException("not supported")

  def createEntityPersistence(entityType: EntityType, crudContext: CrudContext): SeqCrudPersistence[T]

  def setListAdapter(crudType: CrudType, findAllResult: Seq[AnyRef], contextItems: List[AnyRef], activity: CrudListActivity) {
    class SeqPersistenceAdapter[T <: AnyRef](findAllResult: Seq[AnyRef], contextItems: List[AnyRef], activity: ListActivity)
            extends BaseAdapter with AdapterCaching {
      def entityType = crudType.entityType

      val seq: Seq[T] = findAllResult.asInstanceOf[Seq[T]]

      def getCount: Int = seq.size

      def getItemId(position: Int): ID = getItem(position) match {
        case crudType.entityType.IdField(Some(id)) => id
        case _ => position
      }

      def getItem(position: Int) = seq(position)

      def getView(position: Int, convertView: View, parent: ViewGroup): View = {
        val view = if (convertView == null) activity.getLayoutInflater.inflate(crudType.rowLayout, parent, false) else convertView
        bindViewFromCacheOrItems(view, getItem(position) :: contextItems, position, activity)
        view
      }
    }

    activity.setListAdapter(new SeqPersistenceAdapter[T](findAllResult, contextItems, activity))
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