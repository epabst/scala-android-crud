package com.github.scala.android.crud.view

import android.view.View
import android.view.View.OnClickListener
import android.net.Uri
import com.github.scala.android.crud.common.UriPath

/** Useful conversions for Android development. */
object AndroidConversions {
  import scala.collection.JavaConversions._
  implicit def toUriPath(uri: Uri): UriPath = UriPath(uri.getPathSegments.toList:_*)

  implicit def toUri(uriPath: UriPath): Uri =
    uriPath.segments.foldLeft(Uri.EMPTY)((uri, segment) => Uri.withAppendedPath(uri, segment))

  implicit def toOnClickListener(body: View => Unit) = new OnClickListener {
    def onClick(view: View) { body(view) }
  }
}
