package com.github.scala.android.crud.view

import com.github.triangle.{PortableValue, Logging}
import com.github.scala.android.crud.persistence.EntityType
import com.github.scala.android.crud.common.Timing
import com.github.triangle.JavaUtil.toRunnable
import android.view.{ViewGroup, View}
import android.widget.BaseAdapter

trait AdapterCaching extends Logging with Timing { self: BaseAdapter =>
  def entityType: EntityType

  protected def logTag = entityType.logTag

  private def findCachedPortableValue(listView: ViewGroup, position: Long): Option[PortableValue] =
    Option(listView.getTag.asInstanceOf[Map[Long, PortableValue]]).flatMap(_.get(position))

  private def cachePortableValue(listView: ViewGroup, position: Long, portableValue: PortableValue) {
    val map = Option(listView.getTag.asInstanceOf[Map[Long, PortableValue]]).getOrElse(Map.empty[Long, PortableValue]) +
      (position -> portableValue)
    listView.setTag(map)
    trace("Added value at position " + position + " to the " + listView + " cache for " + entityType)
  }

  protected[crud] def bindViewFromCacheOrItems(view: View, entity: => AnyRef, contextItems: List[AnyRef], position: Long, listView: ViewGroup) {
    val cachedValue: Option[PortableValue] = findCachedPortableValue(listView, position)
    //set the cached or default values immediately instead of showing the column header names
    cachedValue match {
      case Some(portableValue) =>
        trace("cache hit for " + listView + " of " + entityType + " at position " + position + ": " + portableValue)
        portableValue.copyTo(view, contextItems)
      case None =>
        trace("cache miss for " + listView + " of " + entityType + " at position " + position)
        entityType.defaultPortableValue.copyTo(view, contextItems)
    }
    if (cachedValue.isEmpty) {
      //copy immediately since in the case of a Cursor, it will be advanced to the next row quickly.
      val positionItems: List[AnyRef] = entity +: contextItems
      cachePortableValue(listView, position, entityType.defaultPortableValue)
      future {
        val portableValue = entityType.copyFromItem(positionItems)
        listView.post {
          cachePortableValue(listView, position, portableValue)
          notifyDataSetChanged()
        }
      }
    }
  }
}

object AdapterCaching {
  def clearCache(adapterView: View) {
    adapterView.post { adapterView.setTag(null) }
  }
}
