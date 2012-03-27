package com.github.scala.android.crud.action

import android.app.Activity
import com.github.scala.android.crud.common.PlatformTypes._
import android.content.{Context, Intent}
import com.github.scala.android.crud.common.UriPath
import com.github.scala.android.crud.view.AndroidConversions._
import android.view.View
import com.github.triangle.Field
import com.github.triangle.PortableField._
import com.github.scala.android.crud.view.ViewRef

/** Represents something that a user can initiate.
  * @author Eric Pabst (epabst@gmail.com)
  * @param icon  The optional icon to display.
  * @param title  The title to display.
  * @param viewRef  The ViewKey (or equivalent) that represents the Command for the user to click on.  Optional.
  *   If the title is None, it can't be displayed in a context menu for a list item.
  *   If both title and icon are None,
  *   then it can't be displayed in the main options menu, but can still be triggered as a default.
  */
case class Command(icon: Option[ImgKey], title: Option[SKey], viewRef: Option[ViewRef] = None) {
  /** A CommandID that can be used to identify if it's the same as another in a list.
    * It uses the title or else the icon or else the hash code.
    */
  def commandId: CommandId = title.orElse(icon).getOrElse(##)
}

/** Represents an operation that a user can initiate. */
trait Operation {
  /** Runs the operation, given the uri and the current state of the application. */
  def invoke(uri: UriPath, activity: ActivityWithVars)
}

object Operation {
  val CreateActionName = Intent.ACTION_INSERT
  val ListActionName = Intent.ACTION_PICK
  val DisplayActionName = Intent.ACTION_VIEW
  val UpdateActionName = Intent.ACTION_EDIT
  val DeleteActionName = Intent.ACTION_DELETE

  implicit def toRichItent(intent: Intent) = new RichIntent(intent)

  //this is a workaround because Robolectric doesn't handle the full constructor
  def constructIntent(action: String, uriPath: UriPath, context: Context, clazz: Class[_]): Intent = {
    val intent = new Intent(action, uriPath)
    intent.setClass(context, clazz)
    intent
  }
}

/** Represents an action that a user can initiate.
  * It's equals/hashCode MUST be implemented in order to suppress the action that is already happening.
  */
case class Action(command: Command, operation: Operation) {
  def commandId: CommandId = command.commandId

  def invoke(uri: UriPath, activity: ActivityWithVars) {
    operation.invoke(uri, activity)
  }
}

case class RichIntent(intent: Intent) {
  def uriPath: UriPath = intent.getData
}

trait StartActivityOperation extends Operation {
  def determineIntent(uri: UriPath, activity: ActivityWithVars): Intent

  def invoke(uri: UriPath, activity: ActivityWithVars) {
    activity.startActivity(determineIntent(uri, activity))
  }
}

trait BaseStartActivityOperation extends StartActivityOperation {
  def action: String

  def activityClass: Class[_ <: Activity]

  def determineIntent(uri: UriPath, activity: ActivityWithVars): Intent = Operation.constructIntent(action, uri, activity, activityClass)
}

/** An Operation that starts an Activity using the provided Intent.
  * @param intent the Intent to use to start the Activity.  It is pass-by-name because the SDK's Intent has a "Stub!" error. */
class StartActivityOperationFromIntent(intent: => Intent) extends StartActivityOperation {
  def determineIntent(uri: UriPath, activity: ActivityWithVars) = intent
}

//final to guarantee equality is correct
final case class StartNamedActivityOperation(action: String, activityClass: Class[_ <: Activity]) extends BaseStartActivityOperation

trait EntityOperation extends Operation {
  def entityName: String
  def action: String
}

//final to guarantee equality is correct
final case class StartEntityActivityOperation(entityName: String, action: String, activityClass: Class[_ <: Activity])
  extends BaseStartActivityOperation with EntityOperation {

  override def determineIntent(uri: UriPath, activity: ActivityWithVars): Intent =
    super.determineIntent(uri.specify(entityName), activity)
}

//final to guarantee equality is correct
final case class StartEntityIdActivityOperation(entityName: String, action: String, activityClass: Class[_ <: Activity])
  extends BaseStartActivityOperation with EntityOperation {

  override def determineIntent(uri: UriPath, activity: ActivityWithVars) = super.determineIntent(uri.upToIdOf(entityName), activity)
}

trait StartActivityForResultOperation extends StartActivityOperation {
  def viewIdToRespondTo: ViewKey

  override def invoke(uri: UriPath, activity: ActivityWithVars) {
    activity.startActivityForResult(determineIntent(uri, activity), viewIdToRespondTo)
  }
}

object StartActivityForResultOperation {
  def apply(view: View, intent: => Intent): StartActivityForResultOperation =
    new StartActivityOperationFromIntent(intent) with StartActivityForResultOperation {
      def viewIdToRespondTo = view.getId
    }
}

/** The response to a [[com.github.scala.android.crud.action.StartActivityForResultOperation]].
  * This is used by [[com.github.scala.android.crud.CrudActivity]]'s startActivityForResult.
  */
case class OperationResponse(viewIdRespondingTo: ViewKey, intent: Intent)

/** An extractor to get the OperationResponse from the items being copied from. */
object OperationResponseExtractor extends Field(identityField[OperationResponse])
