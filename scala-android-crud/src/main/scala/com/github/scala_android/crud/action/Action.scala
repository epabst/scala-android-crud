package com.github.scala_android.crud.action

import android.app.Activity
import com.github.scala_android.crud.common.PlatformTypes
import android.net.Uri
import android.content.{Context, Intent}
import collection.JavaConversions
import com.github.triangle.ValueFormat._

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
  def invoke(uri: Uri, activity: Activity)
}

abstract class RunnableAction(val icon: Option[PlatformTypes#ImgKey], val title: Option[PlatformTypes#SKey]) extends Action

object Action {
  val CreateActionName = Intent.ACTION_INSERT
  val ListActionName = Intent.ACTION_PICK
  val DisplayActionName = Intent.ACTION_VIEW
  val UpdateActionName = Intent.ACTION_EDIT
  val DeleteActionName = Intent.ACTION_DELETE

  def toUri(segments: String*): Uri = segments.foldLeft(Uri.EMPTY)((uri, segment) => Uri.withAppendedPath(uri, segment))

  //this is a workaround because Robolectric doesn't handle the full constructor
  def constructIntent(action: String, uri: Uri, context: Context, clazz: Class[_]): Intent = {
    val intent = new Intent(action, uri)
    intent.setClass(context, clazz)
    intent
  }
}

trait StartActivityAction extends Action {
  def action: String

  def activityClass: Class[_ <: Activity]

  def determineIntent(uri: Uri, activity: Activity): Intent = Action.constructIntent(action, uri, activity, activityClass)

  def invoke(uri: Uri, activity: Activity) {
    activity.startActivity(determineIntent(uri, activity))
  }
}

case class EntityUriSegment(entityName: String, detail: String*) extends PlatformTypes {
  import JavaConversions._
  private val idFormat = basicFormat[ID]

  def specifyInUri(currentUri: Uri): Uri =
    replacePathSegments(currentUri, _.takeWhile(_ != entityName) ::: entityName :: detail.toList)

  def findId(currentUri: Uri): Option[ID] =
    currentUri.getPathSegments.toList.dropWhile(_ != entityName) match {
      case nameString :: idString :: x => idFormat.toValue(idString)
      case _ => None
    }

  def keepUpToTheIdInUri(currentUri: Uri): Uri =
    EntityUriSegment(entityName, findId(currentUri).get.toString).specifyInUri(currentUri)

  private def replacePathSegments(uri: Uri, f: List[String] => List[String]): Uri = {
    val path = f(uri.getPathSegments.toList)
    Action.toUri(path: _*)
  }
}

//final to guarantee equality is correct
final case class StartNamedActivityAction(action: String,
                                          icon: Option[PlatformTypes#ImgKey], title: Option[PlatformTypes#SKey],
                                          activityClass: Class[_ <: Activity]) extends StartActivityAction

//final to guarantee equality is correct
final case class StartEntityActivityAction(entityUriSegment: EntityUriSegment, action: String,
                                           icon: Option[PlatformTypes#ImgKey], title: Option[PlatformTypes#SKey],
                                           activityClass: Class[_ <: Activity]) extends StartActivityAction {
  override def determineIntent(uri: Uri, activity: Activity): Intent =
    super.determineIntent(entityUriSegment.specifyInUri(uri), activity)
}

//final to guarantee equality is correct
final case class StartEntityIdActivityAction(entityName: String, action: String,
                                             icon: Option[PlatformTypes#ImgKey], title: Option[PlatformTypes#SKey],
                                             activityClass: Class[_ <: Activity]) extends StartActivityAction {
  val entityUriSegmentWithoutId = EntityUriSegment(entityName)

  override def determineIntent(uri: Uri, activity: Activity) =
    super.determineIntent(entityUriSegmentWithoutId.keepUpToTheIdInUri(uri), activity)
}
