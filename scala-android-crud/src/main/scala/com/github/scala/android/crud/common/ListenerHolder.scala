package com.github.scala.android.crud.common

/**
 * A Listener holder
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 8/16/11
 * Time: 10:41 PM
 */

trait ListenerHolder[L] {
  private var theListeners = Set[L]()

  protected def listeners: Set[L] = theListeners

  def addListener(listener: L) {
    theListeners = theListeners + listener
  }

  def removeListener(listener: L) {
    theListeners = theListeners - listener
  }
}