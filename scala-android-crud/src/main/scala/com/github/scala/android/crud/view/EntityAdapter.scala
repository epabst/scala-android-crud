package com.github.scala.android.crud.view

import android.app.ListActivity
import android.widget.BaseAdapter
import com.github.scala.android.crud.common.PlatformTypes._
import android.view.{ViewGroup, View}
import scala.Predef._
import com.github.scala.android.crud.persistence.EntityType

/** An Android Adapter for an EntityType with the result of EntityPersistence.findAll.
  * @author Eric Pabst (epabst@gmail.com)
  */
class EntityAdapter(val entityType: EntityType, values: Seq[AnyRef], rowLayout: ViewKey,
                    contextItems: List[AnyRef], activity: ListActivity) extends BaseAdapter with AdapterCaching {
  def getCount: Int = values.size

  def getItemId(position: Int): ID = getItem(position) match {
    case entityType.IdField(Some(id)) => id
    case _ => position
  }

  def getItem(position: Int) = values(position)

  def getView(position: Int, convertView: View, parent: ViewGroup): View = {
    val view = if (convertView == null) activity.getLayoutInflater.inflate(rowLayout, parent, false) else convertView
    bindViewFromCacheOrItems(view, getItem(position), contextItems, position, activity)
    view
  }
}
