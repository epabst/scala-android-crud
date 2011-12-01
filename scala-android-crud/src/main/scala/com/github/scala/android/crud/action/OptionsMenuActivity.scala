package com.github.scala.android.crud.action

import android.view.Menu
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicBoolean

/**
 * An Activity that has an options menu.
 * This is intended to handle both Android 2 and 3.
 * The options menu in Android 3 can be left visible all the time until invalidated.
 * When the options menu changes, invoke {{{this.optionsMenu = ...}}}
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 11/30/11
 * Time: 7:48 PM
 */
trait OptionsMenuActivity[T <: MenuAction] extends ActivityWithVars {
  protected def initialOptionsMenu: List[T]

  // Use a ContextVar instead of a var to make it thread-safe
  private object OptionsMenuVar extends ContextVar[List[T]]

  final def optionsMenu: List[T] = OptionsMenuVar.get(this).getOrElse(initialOptionsMenu)

  def optionsMenu_=(newValue: List[T]) {
    OptionsMenuVar.set(this, newValue)
    invalidateOptionsMenuMethod.map(_.invoke(this)).getOrElse(recreateInPrepare.set(true))
  }

  private val recreateInPrepare = new AtomicBoolean(false)
  private lazy val invalidateOptionsMenuMethod: Option[Method] =
    try { Option(getClass.getMethod("invalidateOptionsMenu"))}
    catch { case _ => None }

  private[action] def populateMenu(menu: Menu, actions: List[T]) {
    for ((action, index) <- actions.zip(Stream.from(0))) {
      val menuItem = action.title.map(menu.add(0, action.actionId, index, _)).getOrElse(menu.add(0, action.actionId, index, ""))
      action.icon.map(icon => menuItem.setIcon(icon))
    }
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    populateMenu(menu, optionsMenu)
    true
  }

  override def onPrepareOptionsMenu(menu: Menu) = {
    if (recreateInPrepare.getAndSet(false)) {
      menu.clear()
      populateMenu(menu, optionsMenu)
      true
    } else {
      super.onPrepareOptionsMenu(menu)
    }
  }
}
