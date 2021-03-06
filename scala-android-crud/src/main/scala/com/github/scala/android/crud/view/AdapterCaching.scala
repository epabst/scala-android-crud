package com.github.scala.android.crud.view

import com.github.triangle.{PortableValue, Logging}
import com.github.scala.android.crud.persistence.EntityType
import android.view.{ViewGroup, View}
import com.github.scala.android.crud.CachedStateListener
import android.os.Bundle
import android.widget.{Adapter, AdapterView, BaseAdapter}
import android.util.SparseArray
import collection.mutable
import com.github.scala.android.crud.common.{Common, Timing}
import actors.Actor

case class GetValueAtPosition(position: Long, entityData: AnyRef, contextItems: scala.List[AnyRef], onCached: PortableValue => Unit)
case class CacheValue(position: Long, portableValue: PortableValue, onCached: PortableValue => Unit)
case class ClearCache(reason: String)
object RetrieveCachedState
case class CachedState(bundles: Map[Long,Bundle])

class CacheActor(adapterView: ViewGroup, adapter: BaseAdapter, entityType: EntityType) extends Actor with Logging with Timing { self =>
  protected def logTag = entityType.logTag
  private var cache: Map[Long, PortableValue] = Map.empty

  /** Logs every message as it is about to be handled. */
  def withMessageLogging(f: PartialFunction[Any,Unit]): PartialFunction[Any,Unit] = {
    case message if f.isDefinedAt(message) =>
      debug("CacheActor handling message: " + message.toString)
      f(message)
  }

  def act() {
    loop {
      react(withMessageLogging {
        // This is requested by an AdapterCaching when a View for a position is requested
        case GetValueAtPosition(position, entityData, contextItems, onCached) =>
          val cachedValueOpt = cache.get(position)
          val portableValue = cachedValueOpt match {
            case None =>
              trace("first cache miss for " + adapterView + " of " + entityType + " at position " + position)
              // This prevents duplication of calculating the value in the background.
              cache += position -> entityType.DefaultPortableValue
              future {
                val positionItems: List[AnyRef] = entityData +: contextItems
                val portableValue = entityType.copyFromItem(positionItems)
                // onCached should cause bindViewFromCacheOrItems to be requested again
                this ! CacheValue(position, portableValue, onCached)
              }
              entityType.DefaultPortableValue
            case Some(entityType.DefaultPortableValue) =>
              trace("another cache miss for " + adapterView + " of " + entityType + " at position " + position)
              entityType.DefaultPortableValue
            case Some(cachedValue) =>
              trace("cache hit for " + adapterView + " of " + entityType + " at position " + position + ": " + cachedValue)
              cachedValue
          }
          self.reply(portableValue)
        case CacheValue(position, portableValue, onCached) =>
          trace("Added value at position " + position + " to the " + adapterView + " cache for " + entityType)
          cache += position -> portableValue
          onCached(portableValue)
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
          // This will result in a bindViewFromCacheOrItems request for all visible Views
          adapterView.postInvalidate()
        case ClearCache(reason) =>
          cache = Map.empty
          trace("Clearing cache in " + adapterView + " of " + entityType + " due to " + reason)
          // This will result in a bindViewFromCacheOrItems request for all visible Views
          adapterView.postInvalidate()
      })
    }
  }
}

object AdapterCaching extends Timing {
  protected def logTag = Common.logTag

  /** This should be run on the UI Thread. */
  private[crud] def findCacheActor(adapterView: ViewGroup): Option[CacheActor] =
    Option(adapterView.getTag.asInstanceOf[CacheActor])

  def clearCache(adapterView: ViewGroup, reason: String) {
    runOnUiThread(adapterView) {
      findCacheActor(adapterView).foreach { cacheActor =>
        cacheActor ! ClearCache(reason)
      }
    }
  }
}

trait AdapterCaching extends Logging with Timing { self: BaseAdapter =>
  import AdapterCaching._

  def entityType: EntityType

  protected def logTag = entityType.logTag

  private def cacheActor(adapterView: ViewGroup): CacheActor = {
    findCacheActor(adapterView).getOrElse {
      val actor = new CacheActor(adapterView, this, entityType)
      adapterView.setTag(actor)
      actor.start()
      actor
    }
  }

  protected[crud] def bindViewFromCacheOrItems(view: View, entityData: AnyRef, contextItems: List[AnyRef], position: Long, adapterView: ViewGroup) {
    view.setTag(position)
    // notifyDataSetChanged() should cause bindViewFromCacheOrItems to be requested again once the real value is cached
    (cacheActor(adapterView) !? GetValueAtPosition(position, entityData, contextItems, portableValue => {
      runOnUiThread(adapterView) {
        // Don't copy if the View has been re-used for a different position.
        if (view.getTag == position) {
          portableValue.copyTo(view, contextItems)
        } else {
          notifyDataSetChanged()
        }
      }
    })) match {
      case portableValue: PortableValue =>
        //set the cached or default values immediately.  Default values is better than leaving as-is because the view might have other unrelated data.
        portableValue.copyTo(view, contextItems)
    }
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
    //todo create CacheActor if it doesn't exist
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
