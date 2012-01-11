package com.github.scala.android.crud.action

import collection.mutable.ConcurrentMap
import java.util.concurrent.ConcurrentHashMap
import android.content.Context
import collection.JavaConversions._
import android.app.Activity
import com.github.scala.android.crud.DestroyContextListener
import com.github.scala.android.crud.common.ListenerHolder

/** A container for values of [[com.github.scala.android.crud.action.ContextVar]]'s */
trait ContextVars extends ListenerHolder[DestroyContextListener] {
  //for some reason, making this lazy results in it being null during testing, even though lazy would be preferrable.
  private[crud] val variables: ConcurrentMap[ContextVar[_], Any] = new ConcurrentHashMap[ContextVar[_], Any]()

  def onDestroyContext() {
    listeners.foreach(_.onDestroyContext())
  }
}

/** A variable stored in a [[com.github.scala.android.crud.action.ContextWithVars]].
  * <p />
  * Normally you create an object that extends this:
  * {{{object ProductName extends ContextVar[String]}}}
  * But if you need uniqueness by instance, do this:
  * {{{val productName = new ContextVar[String]}}}
  * It doesn't accumulate any data and is sharable across threads since all data is stored in each CrudContext.
  */
class ContextVar[T] {
  /** Gets the value or None if not set.
    * @param context the Context where the value is stored
    * @return Some(value) if set, otherwise None
    */
  def get(context: ContextVars): Option[T] = {
    context.variables.get(this).map(_.asInstanceOf[T])
  }

  /** Tries to set the value in {{{crudContext}}}.
    * @param context the Context where the value is stored
    * @param value the value to set in the Context.
    */
  def set(context: ContextVars, value: T) {
    context.variables.put(this, value)
  }

  def clear(context: ContextVars): Option[T] = {
    context.variables.remove(this).map(_.asInstanceOf[T])
  }

  def getOrSet(context: ContextVars, initialValue: => T): T = {
    get(context).getOrElse {
      val value = initialValue
      set(context, value)
      value
    }
  }
}

/** A ContextVars that has been mixed with a Context.
  * @author Eric Pabst (epabst@gmail.com)
  */
trait ContextWithVars extends Context with ContextVars

/** A ContextVars that has been mixed with an Activity. */
trait ActivityWithVars extends Activity with ContextWithVars
