package com.github.scala_android.crud

import res.R
import android.widget.BaseAdapter
import android.view.{ViewGroup, View}
import com.github.triangle.PortableField.identityField
import com.github.triangle.JavaUtil.toRunnable
import android.app.ListActivity

trait GeneratedCrudType[T <: AnyRef] extends CrudType {
  def newWritable = throw new UnsupportedOperationException("not supported")

  def openEntityPersistence(crudContext: CrudContext): ListEntityPersistence[T]

  def setListAdapter(persistence: CrudPersistence, crudContext: CrudContext, activity: ListActivity) {
    activity.setListAdapter(new BaseAdapter() {
      val listPersistence = persistence.asInstanceOf[ListEntityPersistence[T]]
      val list: List[T] = {
        val criteria = listPersistence.newCriteria
        copy(activity.getIntent, criteria)
        listPersistence.findAll(criteria)
      }

      def getCount: Int = list.size

      def getItemId(position: Int) = listPersistence.getId(list(position))

      def getItem(position: Int) = list(position)

      def getView(position: Int, convertView: View, parent: ViewGroup): View = {
        val contextItems = List(activity.getIntent, crudContext, Unit)
        val view = if (convertView == null) activity.getLayoutInflater.inflate(rowLayout, parent, false) else convertView
        future {
          val portableValue = copyFrom(list(position) :: contextItems)
          view.post { portableValue.copyTo(view) }
        }
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