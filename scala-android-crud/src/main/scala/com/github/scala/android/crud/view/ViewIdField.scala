package com.github.scala.android.crud.view

import com.github.triangle.PortableField._
import android.view.View
import android.app.Activity
import com.github.triangle.{Getter, PartialDelegatingField, PortableField}
import com.github.scala.android.crud.action.OperationResponse

/** PortableField for a View resource within a given parent View */
class ViewIdField[T](val viewRef: ViewRef, childViewField: PortableField[T]) extends PartialDelegatingField[T] {
  private lazy val viewKeyMapField: PortableField[T] =
    viewRef.viewKeyOpt.map { key =>
      Getter[ViewKeyMap,T](_.get(key).map(_.asInstanceOf[T])).withTransformer(map => value => map + (key -> value), _ - key)
    }.getOrElse(emptyField)

  def withViewKeyMapField: PortableField[T] = this + viewKeyMapField

  object ChildView {
    def unapply(target: Any): Option[View] = target match {
      case view: View =>
        // uses the "Alternative to the ViewHolder" pattern: http://www.screaming-penguin.com/node/7767#comment-16978
        viewRef.viewKeyOpt.flatMap(id => Option(view.getTag(id).asInstanceOf[View]).orElse {
          val foundView = Option(view.findViewById(id))
          foundView.foreach(view.setTag(id, _))
          foundView
        })
      case activity: Activity => viewRef.viewKeyOpt.flatMap(id => Option(activity.findViewById(id)))
      case _ => None
    }
  }

  protected def delegate = childViewField

  private lazy val GivenViewId = viewRef.viewKeyOpt.getOrElse(View.NO_ID)

  protected def subjectGetter = {
    case ChildView(childView) =>
      childView
    case actionResponse @ OperationResponse(GivenViewId, _) =>
      actionResponse
  }

  override def toString = "viewId(" + viewRef + ", " + childViewField + ")"
}
