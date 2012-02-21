package com.github.scala.android.crud.common

import actors.Futures
import android.view.View
import android.app.Activity

/** A utility for interacting with threads, which enables overriding for testing.
  * @author Eric Pabst (epabst@gmail.com)
  */

trait Timing {
  def future[T](body: => T): scala.actors.Future[T] = {
    Futures.future(body)
  }

  def runOnUiThread[T](view: View)(body: => T) {
    view.post(toRunnable(body))
  }

  def runOnUiThread[T](activity: Activity)(body: => T) {
    activity.runOnUiThread(toRunnable(body))
  }

  private def toRunnable(operation: => Unit): Runnable = new Runnable {
    def run() {
      operation
    }
  }
}
