package com.github.scala_android.crud

import android.app.Activity

/**
 * Support for the different Crud Activity's.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/25/11
 * Time: 7:01 PM
 */

trait CrudContext[Q <: AnyRef,L <: AnyRef,R <: AnyRef,W <: AnyRef] { this: Activity =>
  def entityConfig: CrudEntityConfig[Q,L,R,W]

  protected val activity: Activity = this

  lazy val actionFactory = new ActivityUIActionFactory(this)
}
