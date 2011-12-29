package com.github.scala.android.crud.common

import java.util.concurrent.ConcurrentHashMap
import collection.mutable.ConcurrentMap
import scala.collection.JavaConversions._
import scala.collection.Set

/** A Listener holder
  * @author Eric Pabst (epabst@gmail.com)
  */

trait ListenerHolder[L] {
  private val theListeners: ConcurrentMap[L,L] = new ConcurrentHashMap[L,L]()

  protected def listeners: Set[L] = theListeners.keySet

  def addListener(listener: L) {
    theListeners += listener -> listener
  }

  def removeListener(listener: L) {
    theListeners -= listener
  }
}