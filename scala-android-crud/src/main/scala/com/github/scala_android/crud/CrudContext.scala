package com.github.scala_android.crud

import android.content.Context
import android.app.Activity
import scala.collection.mutable

/**
 * A context which can store data for the duration of a single Activity.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 4/2/11
 * Time: 3:43 PM
 */

class CrudContext(val activity: Activity) {
  def context: Context = activity

  private[crud] val variables = mutable.Map[ContextVar[_], AnyVal]()
}

/**
 * A variable stored in a CrudContext.
 * <p />
 * Normally you create an object that extends this:
 * <code>object ProductName extends ContextVar[String]</code>
 * It doesn't accumulate any data and is sharable across threads since all data is stored in each CrudContext.
 */
trait ContextVar[T] {
  /**
   * Gets the value or None if not set.
   * @param crudContext the Context where the value is stored
   * @returns Some(value) if set, otherwise None
   */
  def get(crudContext: CrudContext): Option[T] = {
    crudContext.variables.get(this).map(_.asInstanceOf[T])
  }

  /**
   * Tries to set the value in <code>crudContext</code>.
   * @param crudContext the Context where the value is stored
   * @param the value to set in the Context.
   */
  def set(crudContext: CrudContext, value: T) {
    crudContext.variables.put(this, value.asInstanceOf[AnyVal])
  }

  def clear(crudContext: CrudContext) {
    crudContext.variables.remove(this)
  }
}
