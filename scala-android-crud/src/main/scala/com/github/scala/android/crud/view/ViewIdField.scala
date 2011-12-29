package com.github.scala.android.crud.view

import com.github.scala.android.crud.common.PlatformTypes._
import com.github.triangle.PortableField._
import android.view.View
import android.app.Activity
import com.github.scala.android.crud.view.AndroidResourceAnalyzer._
import com.github.triangle.{Getter, PartialDelegatingField, PortableField}
import com.github.scala.android.crud.action.OperationResponse

/** PortableField for a View resource within a given parent View */
protected abstract class BaseViewIdField[T](childViewField: PortableField[T])
        extends PartialDelegatingField[T] {
  protected def viewResourceIdOpt: Option[ViewKey]
  protected def viewResourceIdOrError: ViewKey

  private lazy val viewKeyMapField: PortableField[T] =
    viewResourceIdOpt.map { key =>
      Getter[ViewKeyMap,T](_.get(key).map(_.asInstanceOf[T])).withTransformer(map => value => map + (key -> value), _ - key)
    }.getOrElse(emptyField)

  def withViewKeyMapField: PortableField[T] = this + viewKeyMapField

  object ChildView {
    def unapply(target: Any): Option[View] = target match {
      case view: View => viewResourceIdOpt.flatMap(id => Option(view.findViewById(id)))
      case activity: Activity => viewResourceIdOpt.flatMap(id => Option(activity.findViewById(id)))
      case _ => None
    }
  }

  protected def delegate = childViewField

  private lazy val GivenViewId = viewResourceIdOpt.getOrElse(View.NO_ID)

  protected def subjectGetter = {
    case ChildView(childView) =>
      childView
    case actionResponse @ OperationResponse(GivenViewId, _) =>
      actionResponse
  }
}

/** PortableField for a View resource within a given parent View */
class ViewIdField[T](val viewResourceId: ViewKey, childViewField: PortableField[T])
        extends BaseViewIdField[T](childViewField) {
  protected def viewResourceIdOpt = Some(viewResourceId)
  protected def viewResourceIdOrError = viewResourceId
  override def toString = "viewId(" + viewResourceId + ", " + childViewField + ")"
}

/** PortableField for a View resource within a given parent View.
  * @param rIdClasses a list of R.id classes that may contain the id.
  */
class ViewIdNameField[T](val viewResourceIdName: String, childViewField: PortableField[T], rIdClasses: Seq[Class[_]])
        extends BaseViewIdField[T](childViewField) {
  protected lazy val viewResourceIdOpt = findResourceIdWithName(rIdClasses, viewResourceIdName)
  protected def viewResourceIdOrError = resourceIdWithName(rIdClasses, viewResourceIdName)
  override def toString = "viewId(" + viewResourceIdName + ", " + childViewField + ")"
}
