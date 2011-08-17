package com.github.scala_android.crud

import persistence.{CrudPersistence, SeqEntityPersistence}
import res.R
import android.view.{ViewGroup, View}
import com.github.triangle.PortableField.identityField
import android.app.ListActivity
import android.widget.{ListAdapter, BaseAdapter}

trait GeneratedCrudType[T <: AnyRef] extends CrudType {
  def newWritable = throw new UnsupportedOperationException("not supported")

  def openEntityPersistence(crudContext: CrudContext): SeqEntityPersistence[T]

  class SeqPersistenceAdapter[T <: AnyRef](seqPersistence: SeqEntityPersistence[T], crudContext: CrudContext, activity: ListActivity)
          extends BaseAdapter with AdapterCaching {
    val intent = activity.getIntent
    val contextItems = List(intent, crudContext, Unit)
    val seq: Seq[T] = seqPersistence.findAll(seqPersistence.entityType.transform(seqPersistence.newCriteria, intent))

    def getCount: Int = seq.size

    def getItemId(position: Int): ID = seqPersistence.getId(getItem(position))

    def getItem(position: Int) = seq(position)

    def getView(position: Int, convertView: View, parent: ViewGroup): View = {
      val view = if (convertView == null) activity.getLayoutInflater.inflate(rowLayout, parent, false) else convertView
      bindViewFromCacheOrItems(view, getItem(position) :: contextItems, position, activity)
      view
    }
  }

  def setListAdapter(persistence: CrudPersistence, crudContext: CrudContext, activity: ListActivity) {
    activity.setListAdapter(new SeqPersistenceAdapter[T](persistence.asInstanceOf[SeqEntityPersistence[T]], crudContext, activity))
  }

  def refreshAfterDataChanged(listAdapter: ListAdapter) {}

  override def getListActions(actionFactory: UIActionFactory) = super.getReadOnlyListActions(actionFactory)

  override def getEntityActions(actionFactory: UIActionFactory) = super.getReadOnlyEntityActions(actionFactory)

  override val cancelItemString = R.string.cancel_item
}

object GeneratedCrudType {
  val crudContextField = identityField[CrudContext]
}