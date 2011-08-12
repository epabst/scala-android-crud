package com.github.scala_android.crud.common

import actors.Futures

/**
 * A utility for interacting with threads, which enables overriding for testing.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 7/25/11
 * Time: 9:46 AM
 */

trait Timing {
  def future[T](body: => T): scala.actors.Future[T] = {
    Futures.future(body)
  }
}
