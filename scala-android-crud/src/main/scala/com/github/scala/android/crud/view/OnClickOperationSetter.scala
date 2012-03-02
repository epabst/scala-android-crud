package com.github.scala.android.crud.view

import android.view.View
import com.github.scala.android.crud.action.{ActivityWithVars, Operation}
import com.github.scala.android.crud.common.UriPath
import com.github.triangle.SetterUsingItems
import com.github.scala.android.crud.view.AndroidConversions._
import com.github.scala.android.crud.{CrudContextField, CrudContext}

/** A Setter that invokes an Operation when the View is clicked.
  * @author Eric Pabst (epabst@gmail.com)
  */
case class OnClickOperationSetter[T](viewOperation: View => Operation) extends SetterUsingItems[T]  {
  override def setterUsingItems = {
    case (view: View, CrudContextField(Some(CrudContext(activity: ActivityWithVars, _)))) => ignoredValue => {
      if (view.isClickable) {
        view.setOnClickListener { view: View =>
          viewOperation(view).invoke(UriPath.EMPTY, activity)
        }
      }
    }
  }
}
