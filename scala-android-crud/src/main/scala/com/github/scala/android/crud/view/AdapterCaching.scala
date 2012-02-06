package com.github.scala.android.crud.view

import com.github.triangle.{PortableValue, Logging}
import com.github.scala.android.crud.persistence.EntityType
import com.github.scala.android.crud.common.Timing
import com.github.triangle.JavaUtil.toRunnable
import android.view.{ViewGroup, View}
import actors.Futures.future
import com.github.scala.android.crud.CachedStateListener
import android.os.Bundle
import android.widget.{Adapter, AdapterView, BaseAdapter}
import actors.Actor
import android.util.SparseArray
import collection.mutable

case class CacheValue(position: Long, portableValue: PortableValue)
case class DisplayValueAtPosition(view: View, position: Long, entityData: AnyRef, contextItems: scala.List[AnyRef])
case class ClearCache(reason: String)
object RetrieveCachedState
case class CachedState(bundles: Map[Long,Bundle])

class CacheActor(adapterView: ViewGroup, adapter: BaseAdapter, entityType: EntityType) extends Actor with Logging { self =>
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
          self.reply(Unit)
        case RetrieveCachedState =>
          val state: Map[Long,Bundle] = for ((position, portableValue) <- cache) yield {
            // If entityType doesn't support setting every field (including generated ones) into a Bundle, then the data will be incomplete.
            (position, portableValue.transform[Bundle](new Bundle()))
          }
          self.reply(CachedState(state))
        case CachedState(state) =>
          val portableValues: Map[Long,PortableValue] = for ((position, bundle) <- state) yield {
            // If entityType doesn't support getting every field (including generated ones) from a Bundle, then the data will be incomplete.
            (position, entityType.copyFrom(bundle))
          }
          // Anything in the cache should take precedence over the CachedState
          cache = portableValues ++ cache
          adapterView.post {
            // This will result in a DisplayValueAtPosition request for all visible Views
            adapterView.postInvalidate()
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
  private[crud] def findCacheActor(adapterView: ViewGroup): Option[CacheActor] =
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

class AdapterCachingStateListener[A <: Adapter](adapterView: AdapterView[A], entityType: EntityType, adapterFactory: => A) extends CachedStateListener with Logging {
  protected def logTag = entityType.logTag

  def onSaveState(outState: Bundle) {
    AdapterCaching.findCacheActor(adapterView).foreach { actor =>
      actor !? RetrieveCachedState match {
        case CachedState(state: Map[Long,Bundle]) =>
          //val sparseArray = new SparseArray[Bundle](state.size)
          val sparseArray = new SparseArray[Bundle]()
          for (position <- state.keys) {
            val intPosition = position.asInstanceOf[Int]
            if (intPosition == position) {
              val bundle = state(position)
              sparseArray.put(intPosition, bundle)
            } else {
              logError("position is too high for saving state, not putting into saved state.")
            }
          }
          outState.putSparseParcelableArray(entityType.entityName, sparseArray)
          debug("saved state")
      }
    }
  }

  def onRestoreState(savedInstanceState: Bundle) {
    AdapterCaching.findCacheActor(adapterView).foreach { actor =>
      val sparseArray = savedInstanceState.getSparseParcelableArray[Bundle](entityType.entityName)
      val mapBuffer = mutable.Map.empty[Long,Bundle]
      for (i <- 0 to sparseArray.size() - 1) {
        val position = sparseArray.keyAt(i)
        mapBuffer += ((position.toLong, sparseArray.valueAt(i)))
      }
      actor ! CachedState(mapBuffer.toMap)
    }
  }

  def onClearState(stayActive: Boolean) {
    AdapterCaching.clearCache(adapterView, if (stayActive) "refresh" else "stop")
    if (stayActive) {
      adapterView.setAdapter(adapterFactory)
    }
  }
}
