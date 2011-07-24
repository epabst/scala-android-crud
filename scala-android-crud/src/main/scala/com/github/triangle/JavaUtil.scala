package com.github.triangle

import java.util.HashMap

/**
 * A utility for converting between Scala and Java.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/7/11
 * Time: 6:56 AM
 */

object JavaUtil {
  def toJavaMap[K,V](map: Map[K,Option[V]]): java.util.Map[K,V] = {
    val jMap = new HashMap[K,V]
    for (val (name, value) <- map) {
      jMap.put(name, value.getOrElse(null).asInstanceOf[V])
    }
    jMap
  }

  implicit def toRunnable(operation: => Unit): Runnable = new Runnable {
    def run() {
      operation
    }
  }
}