package com.github.scala.android.crud.view

import android.view.View
import com.github.scala.android.crud.CrudContext
import com.github.scala.android.crud.action.{ActivityWithVars, Operation}
import com.github.scala.android.crud.common.UriPath
import com.github.triangle.{SetterUsingItems, PortableField}
import com.github.scala.android.crud.GeneratedCrudType.CrudContextField
import android.view.View.OnClickListener

/** A Setter that invokes an Operation when the View is clicked.
  * @author Eric Pabst (epabst@gmail.com)
  */
object OnClickOperationSetter {
  private implicit def toOnClickListener(body: View => Unit) = new OnClickListener {
    def onClick(view: View) { body(view) }
  }

  def apply[T](operation: Operation): PortableField[T] = SetterUsingItems[T] {
    case (view: View, CrudContextField(Some(CrudContext(activity: ActivityWithVars, _)))) => ignoredValue => {
      view.setOnClickListener { view: View =>
        operation.invoke(UriPath.EMPTY, activity)
      }
    }
  }
}