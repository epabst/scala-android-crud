package com.github.scala.android.crud.view

import com.github.triangle.{PortableValue, Logging}
import com.github.scala.android.crud.persistence.EntityType
import com.github.scala.android.crud.common.Timing
import com.github.triangle.JavaUtil.toRunnable
import android.view.{ViewGroup, View}
import android.widget.BaseAdapter
import actors.Actor
import actors.Futures.future

case class CacheValue(position: Long, portableValue: PortableValue)
case class DisplayValueAtPosition(view: View, position: Long, entityData: AnyRef, contextItems: scala.List[AnyRef])
case class ClearCache(reason: String)

class CacheActor(adapterView: ViewGroup, adapter: BaseAdapter, entityType: EntityType) extends Actor with Logging {
  protected def logTag = entityType.logTag
  private var cache: Map[Long, PortableValue] = Map.empty

  def act() {
    loop {
      react {
        // This is requested by an AdapterCaching when a View for a position is requested
        case request @ DisplayValueAtPosition(view, position, entityData, contextItems) =>
          val cachedValueOpt = cache.get(position)
          val portableValue = cachedValueOpt match {
            case None =>
              trace("cache miss for " + adapterView + " of " + entityType + " at position " + position)
              future {
                val positionItems: List[AnyRef] = entityData +: contextItems
                val portableValue = entityType.copyFromItem(positionItems)
                this ! CacheValue(position, portableValue)
              }
              entityType.defaultPortableValue
            case Some(cachedValue) =>
              trace("cache hit for " + adapterView + " of " + entityType + " at position " + position + ": " + cachedValue)
              cachedValue
          }
          view.post(toRunnable {
            //set the cached or default values immediately.  Default values is better than leaving as-is because the view might have other unrelated data.
            portableValue.copyTo(view, contextItems)
          })
        case CacheValue(position, portableValue) =>
          trace("Added value at position " + position + " to the " + adapterView + " cache for " + entityType)
          cache += position -> portableValue
          adapterView.post {
            // This will result in a DisplayValueAtPosition request if the View is still visible
            adapter.notifyDataSetChanged()
          }
        case ClearCache(reason) =>
          cache = Map.empty
          trace("Clearing cache in " + adapterView + " of " + entityType + " due to " + reason)
          // This will result in a DisplayValueAtPosition request for all visible Views
          adapterView.postInvalidate()
      }
    }
  }
}

object AdapterCaching {
  /** This should be run in the UI Thread. */
  private def findCacheActor(adapterView: ViewGroup): Option[CacheActor] =
    Option(adapterView.getTag.asInstanceOf[CacheActor])

  def clearCache(adapterView: ViewGroup, reason: String) {
    adapterView.post(toRunnable {
      findCacheActor(adapterView).foreach { cacheActor =>
        cacheActor ! ClearCache(reason)
      }
    })
  }
}

trait AdapterCaching extends Logging with Timing { self: BaseAdapter =>
  import AdapterCaching._

  def entityType: EntityType

  protected def logTag = entityType.logTag

  private def sendMessageToCacheActor(adapterView: ViewGroup, message: Any) {
    adapterView.post(toRunnable {
      val actor = findCacheActor(adapterView).getOrElse {
        val actor = new CacheActor(adapterView, this, entityType)
        adapterView.setTag(actor)
        actor.start()
        actor
      }
      actor ! message
    })
  }

  protected[crud] def bindViewFromCacheOrItems(view: View, entityData: AnyRef, contextItems: List[AnyRef], position: Long, adapterView: ViewGroup) {
    sendMessageToCacheActor(adapterView, DisplayValueAtPosition(view, position, entityData, contextItems))
  }
}
