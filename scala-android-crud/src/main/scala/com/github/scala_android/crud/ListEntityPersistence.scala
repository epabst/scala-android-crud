package com.github.scala_android.crud

import android.app.Activity
import android.widget.BaseAdapter
import android.view.{ViewGroup, View}
import android.content.ContentValues

/**
 * EntityPersistence for a simple generated List.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/11/11
 * Time: 5:05 PM
 */

abstract class ListEntityPersistence[T <: AnyRef,Q <: AnyRef](entityType: CrudEntityType[Q,List[T],T,ContentValues],
                                                              activity: Activity) extends EntityPersistence[Q,List[T],T,ContentValues] {
  lazy val list = {
    val criteria = newCriteria
    entityType.copyFields(activity.getIntent, criteria)
    findAll(criteria)
  }

  def getId(entity: T): ID

  def find(id: ID): Option[T] = list.find(entity => id == getId(entity))

  def save(id: Option[ID], contentValues: ContentValues) =
    throw new UnsupportedOperationException("write not suppoted")

  def delete(ids: List[ID]) = throw new UnsupportedOperationException("delete not supported")

  def close() {}

  def createListAdapter(activity: Activity) = new BaseAdapter() {
    def getCount: Int = list.size

    def getItemId(position: Int) = getId(list(position))

    def getItem(position: Int) = list(position)

    def getView(position: Int, convertView: View, parent: ViewGroup): View = {
      val view = if (convertView == null) activity.getLayoutInflater.inflate(entityType.rowLayout, parent, false) else convertView
      entityType.copyFields(list(position), view)
      view
    }
  }
}