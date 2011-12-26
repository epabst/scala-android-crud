package com.github.scala.android.crud.view

import android.view.View
import android.view.View.OnClickListener

/** Useful conversions for Android development. */
object AndroidConversions {
  implicit def toOnClickListener(body: View => Unit) = new OnClickListener {
    def onClick(view: View) { body(view) }
  }
}
