package com.github.scala.android.crud.view

import android.widget.BaseAdapter
import android.app.ListActivity
import com.github.triangle.{PortableValue, Logging}
import com.github.scala.android.crud.persistence.{PersistenceListener, EntityType}
import com.github.scala.android.crud.common.PlatformTypes._
import com.github.scala.android.crud.common.{UriPath, Timing}
import android.view.View
import com.github.triangle.JavaUtil.toRunnable

trait AdapterCaching extends Logging with Timing { self: BaseAdapter =>
  def entityType: EntityType

  protected def logTag = entityType.logTag

  private def findCachedPortableValue(activity: ListActivity, position: Long): Option[PortableValue] =
    Option(activity.getListView.getTag.asInstanceOf[Map[Long, PortableValue]]).flatMap(_.get(position))

  private def cachePortableValue(activity: ListActivity, position: Long, portableValue: PortableValue) {
    val listView = activity.getListView
    val map = Option(listView.getTag.asInstanceOf[Map[Long,PortableValue]]).getOrElse(Map.empty[Long,PortableValue]) +
            (position -> portableValue)
    listView.setTag(map)
    trace("Added value at position " + position + " to the cache for " + activity)
  }

  def cacheClearingPersistenceListener(activity: ListActivity) = new PersistenceListener {
    def onSave(id: ID) {
      trace("Clearing ListView cache in " + activity + " since DataSet was invalidated")
      activity.runOnUiThread { activity.getListView.setTag(null) }
    }

    def onDelete(uri: UriPath) {
      trace("Clearing ListView cache in " + activity + " since DataSet was invalidated")
      activity.runOnUiThread { activity.getListView.setTag(null) }
    }
  }

  protected[crud] def bindViewFromCacheOrItems(view: View, entity: => AnyRef, contextItems: List[AnyRef], position: Long, activity: ListActivity) {
    val cachedValue: Option[PortableValue] = findCachedPortableValue(activity, position)
    //set the cached or default values immediately instead of showing the column header names
    cachedValue match {
      case Some(portableValue) =>
        trace("cache hit for " + activity + " at position " + position + ": " + portableValue)
        portableValue.copyTo(view, contextItems)
      case None =>
        trace("cache miss for " + activity + " at position " + position)
        entityType.defaultPortableValue.copyTo(view, contextItems)
    }
    if (cachedValue.isEmpty) {
      //copy immediately since in the case of a Cursor, it will be advanced to the next row quickly.
      val positionItems: List[AnyRef] = entity +: contextItems
      cachePortableValue(activity, position, entityType.defaultPortableValue)
      future {
        val portableValue = entityType.copyFromItem(positionItems)
        activity.runOnUiThread {
          cachePortableValue(activity, position, portableValue)
          notifyDataSetChanged()
        }
      }
    }
  }
}
