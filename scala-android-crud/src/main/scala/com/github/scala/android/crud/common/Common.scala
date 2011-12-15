package com.github.scala.android.crud.common

/**
 * Common functionality that are use throughout scala-android-crud.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 11/11/11
 * Time: 6:34 AM
 */

object Common {
  val logTag = "scala-android-crud"

  /** Evaluates the given function and returns the result.  If it throws an exception, it returns None. */
  def tryToEvaluate[T](f: => T): Option[T] = {
    try { Some(f) }
    catch { case _ => None }
  }
}