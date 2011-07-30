package com.github.scala_android.crud

import res.R
import android.view.{ViewGroup, View}
import com.github.triangle.PortableField.identityField
import android.app.ListActivity
import android.widget.BaseAdapter

trait GeneratedCrudType[T <: AnyRef] extends CrudType {
  def newWritable = throw new UnsupportedOperationException("not supported")

  def openEntityPersistence(crudContext: CrudContext): ListEntityPersistence[T]

  class ListPersistenceAdapter[T <: AnyRef](listPersistence: ListEntityPersistence[T], crudContext: CrudContext, activity: ListActivity)
          extends BaseAdapter with AdapterCaching {
    val intent = activity.getIntent
    val contextItems = List(intent, crudContext, Unit)
    val list: List[T] = listPersistence.findAll(listPersistence.entityType.transform(listPersistence.newCriteria, intent))

    def getCount: Int = list.size

    def getItemId(position: Int): ID = listPersistence.getId(list(position))

    def getItem(position: Int) = list(position)

    def getView(position: Int, convertView: View, parent: ViewGroup): View = {
      val view = if (convertView == null) activity.getLayoutInflater.inflate(rowLayout, parent, false) else convertView
      bindViewFromCacheOrItems(view, list(position) :: contextItems, position, activity)
      view
    }
  }

  def setListAdapter(persistence: CrudPersistence, crudContext: CrudContext, activity: ListActivity) {
    activity.setListAdapter(new ListPersistenceAdapter[T](persistence.asInstanceOf[ListEntityPersistence[T]], crudContext, activity))
  }

  def refreshAfterSave(crudContext: CrudContext) {}

  override def getListActions(actionFactory: UIActionFactory) = super.getReadOnlyListActions(actionFactory)

  override def getEntityActions(actionFactory: UIActionFactory) = super.getReadOnlyEntityActions(actionFactory)

  override val cancelItemString = R.string.cancel_item
}

object GeneratedCrudType {
  val crudContextField = identityField[CrudContext]
}