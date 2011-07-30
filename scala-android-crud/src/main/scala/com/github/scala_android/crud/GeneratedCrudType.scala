package com.github.scala_android.crud

import res.R
import android.widget.BaseAdapter
import android.view.{ViewGroup, View}
import com.github.triangle.PortableField.identityField
import android.app.ListActivity

trait GeneratedCrudType[T <: AnyRef] extends CrudType {
  def newWritable = throw new UnsupportedOperationException("not supported")

  def openEntityPersistence(crudContext: CrudContext): ListEntityPersistence[T]

  def setListAdapter(persistence: CrudPersistence, crudContext: CrudContext, activity: ListActivity) {
    val contextItems = List(activity.getIntent, crudContext, Unit)
    activity.setListAdapter(new BaseAdapter() with AdapterCaching {
      val listPersistence = persistence.asInstanceOf[ListEntityPersistence[T]]
      val list: List[T] = listPersistence.findAll(transform(listPersistence.newCriteria, activity.getIntent))

      def getCount: Int = list.size

      def getItemId(position: Int) = listPersistence.getId(list(position))

      def getItem(position: Int) = list(position)

      def getView(position: Int, convertView: View, parent: ViewGroup): View = {
        val view = if (convertView == null) activity.getLayoutInflater.inflate(rowLayout, parent, false) else convertView
        bindViewFromCacheOrItems(view, list(position) :: contextItems, position, activity)
        view
      }
    })
  }

  def refreshAfterSave(crudContext: CrudContext) {}

  override def getListActions(actionFactory: UIActionFactory) = super.getReadOnlyListActions(actionFactory)

  override def getEntityActions(actionFactory: UIActionFactory) = super.getReadOnlyEntityActions(actionFactory)

  override val cancelItemString = R.string.cancel_item
}

object GeneratedCrudType {
  val crudContextField = identityField[CrudContext]
}