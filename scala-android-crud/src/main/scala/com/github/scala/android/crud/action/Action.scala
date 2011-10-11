package com.github.scala.android.crud.action

import android.app.Activity
import com.github.scala.android.crud.common.PlatformTypes
import android.content.{Context, Intent}
import android.net.Uri

/**
 * Represents an action that a user can initiate.
 * It's equals/hashCode MUST be implemented in order to suppress the action that is already happening.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 8/26/11
 * Time: 6:39 AM
 */
trait Action extends PlatformTypes {
  /** The optional icon to display for this action. */
  def icon: Option[ImgKey]

  /**
   * The title to display for this action.
   * If the title is None, it can't be displayed in the context menu for an item.
   * If both title and icon are None,
   * then it can't be displayed in the main options menu, but can still be triggered as a default action.
   */
  def title: Option[SKey]

  /** Runs the action, given the uri and the current state of the application. */
  def invoke(uri: UriPath, activity: Activity)
}

abstract class RunnableAction(val icon: Option[PlatformTypes#ImgKey], val title: Option[PlatformTypes#SKey]) extends Action

object Action {
  val CreateActionName = Intent.ACTION_INSERT
  val ListActionName = Intent.ACTION_PICK
  val DisplayActionName = Intent.ACTION_VIEW
  val UpdateActionName = Intent.ACTION_EDIT
  val DeleteActionName = Intent.ACTION_DELETE

  def toUri(uriPath: UriPath): Uri = uriPath.segments.foldLeft(Uri.EMPTY)((uri, segment) => Uri.withAppendedPath(uri, segment))

  implicit def toRichItent(intent: Intent) = new RichIntent(intent)

  //this is a workaround because Robolectric doesn't handle the full constructor
  def constructIntent(action: String, uriPath: UriPath, context: Context, clazz: Class[_]): Intent = {
    val intent = new Intent(action, toUri(uriPath))
    intent.setClass(context, clazz)
    intent
  }
}

case class RichIntent(intent: Intent) {
  def uriPath: UriPath = UriPath(intent.getData)
}

trait StartActivityAction extends Action {
  def determineIntent(uri: UriPath, activity: Activity): Intent

  def invoke(uri: UriPath, activity: Activity) {
    activity.startActivity(determineIntent(uri, activity))
  }
}

trait BaseStartActivityAction extends StartActivityAction {
  def action: String

  def activityClass: Class[_ <: Activity]

  def determineIntent(uri: UriPath, activity: Activity): Intent = Action.constructIntent(action, uri, activity, activityClass)
}

//final to guarantee equality is correct
final case class StartActivityActionFromIntent(intent: Intent,
                                               icon: Option[PlatformTypes#ImgKey] = None,
                                               title: Option[PlatformTypes#SKey] = None) extends StartActivityAction {
  def determineIntent(uri: UriPath, activity: Activity) = intent
}

//final to guarantee equality is correct
final case class StartNamedActivityAction(action: String,
                                          icon: Option[PlatformTypes#ImgKey], title: Option[PlatformTypes#SKey],
                                          activityClass: Class[_ <: Activity]) extends BaseStartActivityAction

trait EntityAction extends Action {
  def entityName: String
  def action: String
}

//final to guarantee equality is correct
final case class StartEntityActivityAction(entityName: String, action: String,
                                           icon: Option[PlatformTypes#ImgKey], title: Option[PlatformTypes#SKey],
                                           activityClass: Class[_ <: Activity]) extends BaseStartActivityAction with EntityAction {
  override def determineIntent(uri: UriPath, activity: Activity): Intent =
    super.determineIntent(uri.specify(entityName), activity)
}

//final to guarantee equality is correct
final case class StartEntityIdActivityAction(entityName: String, action: String,
                                             icon: Option[PlatformTypes#ImgKey], title: Option[PlatformTypes#SKey],
                                             activityClass: Class[_ <: Activity]) extends BaseStartActivityAction with EntityAction {
  override def determineIntent(uri: UriPath, activity: Activity) = super.determineIntent(uri.upToIdOf(entityName), activity)
}
