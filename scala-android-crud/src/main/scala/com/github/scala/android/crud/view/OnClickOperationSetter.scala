package com.github.scala.android.crud.view

import android.view.View
import com.github.scala.android.crud.CrudContext
import com.github.scala.android.crud.action.{ActivityWithVars, Operation}
import com.github.scala.android.crud.common.UriPath
import com.github.triangle.SetterUsingItems
import com.github.scala.android.crud.GeneratedCrudType.CrudContextField
import com.github.scala.android.crud.view.AndroidConversions._

/** A Setter that invokes an Operation when the View is clicked.
  * @author Eric Pabst (epabst@gmail.com)
  */
case class OnClickOperationSetter[T](operation: Operation) extends SetterUsingItems[T]  {
  override def setterUsingItems = {
    case (view: View, CrudContextField(Some(CrudContext(activity: ActivityWithVars, _)))) => ignoredValue => {
      view.setOnClickListener { view: View =>
        operation.invoke(UriPath.EMPTY, activity)
      }
    }
  }
}
