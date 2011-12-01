package com.github.scala.android.crud.action

import android.app.Activity
import android.view.Menu
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicBoolean

/**
 * An Activity that has an options menu.
 * This is intended to handle both Android 2 and 3.  The options menu in Android 3 can be left visible all the time
 * until invalidated.
 * So, it is essential that when the options menu changes, the code must call {{{invalidateGeneratedOptionsMenu()}}}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 11/30/11
 * Time: 7:48 PM
 */
trait OptionsMenuActivity extends Activity {
  protected def generateOptionsMenu: List[MenuAction]

  // This method must not be named "invalidateOptionsMenu" or it will conflict with the one looked up by reflection
  def invalidateGeneratedOptionsMenu() {
    invalidateOptionsMenuMethod.map(_.invoke(this)).getOrElse(recreateInPrepare.set(true))
  }

  private val recreateInPrepare = new AtomicBoolean(false)
  private lazy val invalidateOptionsMenuMethod: Option[Method] =
    try { Option(getClass.getMethod("invalidateOptionsMenu"))}
    catch { case _ => None }

  private[action] def populateMenu(menu: Menu, actions: List[MenuAction]) {
    for ((action, index) <- actions.zip(Stream.from(0))) {
      val menuItem = action.title.map(menu.add(0, action.actionId, index, _)).getOrElse(menu.add(0, action.actionId, index, ""))
      action.icon.map(icon => menuItem.setIcon(icon))
    }
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    populateMenu(menu, generateOptionsMenu)
    true
  }

  override def onPrepareOptionsMenu(menu: Menu) = {
    if (recreateInPrepare.getAndSet(false)) {
      menu.clear()
      populateMenu(menu, generateOptionsMenu)
      true
    } else {
      super.onPrepareOptionsMenu(menu)
    }
  }
}
