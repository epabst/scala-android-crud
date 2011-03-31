package com.github.scala_android.crud

import android.app.Activity
import monitor.Logging

/**
 * Support for the different Crud Activity's.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/25/11
 * Time: 7:01 PM
 */

trait CrudContext[Q <: AnyRef,L <: AnyRef,R <: AnyRef,W <: AnyRef] extends Logging { this: Activity =>
  def entityType: CrudEntityType[Q,L,R,W]

  def childEntities: List[CrudEntityTypeRef]

  protected val activity: Activity = this

  override lazy val logTag = classOf[CrudContext[Q,L,R,W]].getName + "(" + entityType.entityName + ")"

  //available to be overridden for testing
  def openEntityPersistence(): EntityPersistence[Q,L,R,W] = entityType.openEntityPersistence(activity)

  def withPersistence[T](f: EntityPersistence[Q,L,R,W] => T): T = {
    val persistence = openEntityPersistence()
    try {
      f(persistence)
    } finally {
      persistence.close
    }
  }

  lazy val actionFactory = new ActivityUIActionFactory(this, childEntities)
}
