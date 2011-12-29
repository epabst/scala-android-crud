package com.github.scala.android.crud.common

import actors.Futures

/** A utility for interacting with threads, which enables overriding for testing.
  * @author Eric Pabst (epabst@gmail.com)
  */

trait Timing {
  def future[T](body: => T): scala.actors.Future[T] = {
    Futures.future(body)
  }
}
