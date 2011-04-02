package com.github.scala_android.crud

import android.content.Context
import android.app.Activity

/**
 * A context which can store data for the duration of a single Activity.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 4/2/11
 * Time: 3:43 PM
 */

class CrudContext(val activity: Activity) {
  def context: Context = activity
}