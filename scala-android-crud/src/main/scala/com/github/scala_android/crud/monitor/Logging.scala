package com.github.scala_android.crud.monitor

import android.util.Log

/**
 * A trait to enable logging
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/2/11
 * Time: 6:54 AM
 */

trait Logging {
  lazy val logTag: String = this.getClass.getName

  protected def verbose(f: => String) {
    println("TRACE " + f)
    try Log.v(logTag, f)
    catch { case e: RuntimeException if (e.getMessage == "Stub!") => }
  }

  protected def debug(f: => String) {
    println("DEBUG " + f)
    try Log.d(logTag, f)
    catch { case e: RuntimeException if (e.getMessage == "Stub!") => }
  }

  protected def info(f: => String) {
    println("INFO  " + f)
    try Log.i(logTag, f)
    catch { case e: RuntimeException if (e.getMessage == "Stub!") => }
  }

  protected def warn(f: => String) {
    println("WARN  " + f)
    try Log.w(logTag, f)
    catch { case e: RuntimeException if (e.getMessage == "Stub!") => }
  }

  protected def error(f: => String) {
    println("ERROR " + f)
    try Log.e(logTag, f)
    catch { case e: RuntimeException if (e.getMessage == "Stub!") => }
  }
}