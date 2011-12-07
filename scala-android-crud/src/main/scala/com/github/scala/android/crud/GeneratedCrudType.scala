package com.github.scala.android.crud

import common.UriPath
import android.view.{ViewGroup, View}
import com.github.triangle.PortableField.identityField
import android.app.ListActivity
import android.widget.{ListAdapter, BaseAdapter}
import common.PlatformTypes._
import persistence.EntityType
import com.github.triangle.Field

trait GeneratedPersistenceFactory[T <: AnyRef] extends PersistenceFactory {
  def newWritable = throw new UnsupportedOperationException("not supported")

  def createEntityPersistence(entityType: EntityType, crudContext: CrudContext): SeqCrudPersistence[T]

  def setListAdapter(crudType: CrudType, findAllResult: Seq[AnyRef], contextItems: List[AnyRef], activity: CrudListActivity) {
    class SeqPersistenceAdapter[T <: AnyRef](findAllResult: Seq[AnyRef], contextItems: List[AnyRef], activity: ListActivity)
            extends BaseAdapter with crudType.AdapterCaching {
      val seq: Seq[T] = findAllResult.asInstanceOf[Seq[T]]

      def getCount: Int = seq.size

      def getItemId(position: Int): ID = getItem(position) match {
        case crudType.IdField(Some(id)) => id
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

abstract class GeneratedCrudType[T <: AnyRef](persistenceFactory: GeneratedPersistenceFactory[T]) extends CrudType(persistenceFactory) {
  override def getListActions(application: CrudApplication) = super.getReadOnlyListActions(application)

  override def getEntityActions(application: CrudApplication) = super.getReadOnlyEntityActions(application)
}

object GeneratedCrudType {
  object CrudContextField extends Field(identityField[CrudContext])
  object UriField extends Field(identityField[UriPath])
}