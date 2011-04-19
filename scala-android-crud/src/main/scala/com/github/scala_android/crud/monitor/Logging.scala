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
    try Log.v(logTag, f)
    catch {
      case e: RuntimeException if (e.getMessage == "Stub!") => println("TRACE " + f)
    }
  }

  protected def debug(f: => String) {
    try Log.d(logTag, f)
    catch {
      case e: RuntimeException if (e.getMessage == "Stub!") => println("DEBUG " + f)
    }
  }

  protected def info(f: => String) {
    try Log.i(logTag, f)
    catch {
      case e: RuntimeException if (e.getMessage == "Stub!") => println("INFO  " + f)
    }
  }

  protected def warn(f: => String) {
    try Log.w(logTag, f)
    catch {
      case e: RuntimeException if (e.getMessage == "Stub!") => println("WARN  " + f)
    }
  }

  protected def error(f: => String) {
    try Log.e(logTag, f)
    catch {
      case e: RuntimeException if (e.getMessage == "Stub!") => println("ERROR " + f)
    }
  }

  protected def error(f: => String, e: Throwable) {
    try Log.e(logTag, f, e)
    catch {
      case e: RuntimeException if (e.getMessage == "Stub!") =>
        print("ERROR " + f + ": ")
        e.printStackTrace(Console.out)
    }
  }
}