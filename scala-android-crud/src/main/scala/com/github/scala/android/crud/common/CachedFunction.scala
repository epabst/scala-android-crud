package com.github.scala.android.crud.common

import java.util.concurrent.ConcurrentHashMap
import scala.collection.JavaConversions._
import collection.mutable.ConcurrentMap

/**
 * A cache of results of a function.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 12/8/11
 * Time: 10:39 PM
 */
case class CachedFunction[A,B](function: (A) => B) extends ((A) => B) {
  private val resultByInput: ConcurrentMap[A,B] = new ConcurrentHashMap[A,B]()

  def apply(input: A): B = resultByInput.get(input).getOrElse {
    val result = function(input)
    resultByInput.putIfAbsent(input, result)
    result
  }
}
