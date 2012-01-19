package com.github.scala.android.crud.action

import android.view.Menu
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicBoolean

/** An Activity that has an options menu.
  * This is intended to handle both Android 2 and 3.
  * The options menu in Android 3 can be left visible all the time until invalidated.
  * When the options menu changes, invoke {{{this.optionsMenuCommands = ...}}}
  * @author Eric Pabst (epabst@gmail.com)
  */
trait OptionsMenuActivity extends ActivityWithVars {
  protected def initialOptionsMenuCommands: List[Command]

  // Use a ContextVar to make it thread-safe
  private object OptionsMenuCommandsVar extends ContextVar[List[Command]]

  final def optionsMenuCommands: List[Command] = OptionsMenuCommandsVar.get(this).getOrElse(initialOptionsMenuCommands)

  def optionsMenuCommands_=(newValue: List[Command]) {
    OptionsMenuCommandsVar.set(this, newValue)
    invalidateOptionsMenuMethod.map(_.invoke(this)).getOrElse(recreateInPrepare.set(true))
  }

  private val recreateInPrepare = new AtomicBoolean(false)
  private lazy val invalidateOptionsMenuMethod: Option[Method] =
    try { Option(getClass.getMethod("invalidateOptionsMenu"))}
    catch { case _ => None }

  private[action] def populateMenu(menu: Menu, commands: List[Command]) {
    for ((command, index) <- commands.zip(Stream.from(0))) {
      val menuItem = command.title.map(menu.add(0, command.commandId, index, _)).getOrElse(menu.add(0, command.commandId, index, ""))
      command.icon.map(icon => menuItem.setIcon(icon))
    }
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    populateMenu(menu, optionsMenuCommands)
    true
  }

  override def onPrepareOptionsMenu(menu: Menu) = {
    if (recreateInPrepare.getAndSet(false)) {
      menu.clear()
      populateMenu(menu, optionsMenuCommands)
      true
    } else {
      super.onPrepareOptionsMenu(menu)
    }
  }
}
