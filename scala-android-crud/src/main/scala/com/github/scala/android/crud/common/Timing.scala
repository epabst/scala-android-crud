package com.github.scala.android.crud.common

import actors.Futures
import android.view.View
import android.app.Activity
import com.github.triangle.Logging

/** A utility for interacting with threads, which enables overriding for testing.
  * @author Eric Pabst (epabst@gmail.com)
  */

trait Timing extends Logging {
  private def withExceptionLogging[T](body: => T): T = {
    try {
      body
    } catch {
      case e =>
        logError("Error in non-UI Thread", e)
        throw e
    }
  }

  def future[T](body: => T): scala.actors.Future[T] = {
    Futures.future(withExceptionLogging(body))
  }

  def runOnUiThread[T](view: View)(body: => T) {
    view.post(toRunnable(withExceptionLogging(body)))
  }

  def runOnUiThread[T](activity: Activity)(body: => T) {
    activity.runOnUiThread(toRunnable(withExceptionLogging(body)))
  }

  private def toRunnable(operation: => Unit): Runnable = new Runnable {
    def run() {
      operation
    }
  }
}
