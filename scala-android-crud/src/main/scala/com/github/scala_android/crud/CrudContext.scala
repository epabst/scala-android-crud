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

  //available to be overridden for testing
  def openEntityPersistence(): EntityPersistence[Q,L,R,W] = entityConfig.openEntityPersistence(activity)

  def withPersistence[T](f: EntityPersistence[Q,L,R,W] => T): T = {
    val persistence = openEntityPersistence()
    try {
      f(persistence)
    } finally {
      persistence.close
    }
  }

  lazy val actionFactory = new ActivityUIActionFactory(this)
}
