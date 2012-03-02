package com.github.scala.android.crud

import action.{Operation, ActivityWithVars}
import common.UriPath
import persistence.EntityType

/** An operation that interacts with an entity's persistence.
  * The CrudContext is available as persistence.crudContext to implementing classes.
  * @author Eric Pabst (epabst@gmail.com)
  */
abstract class PersistenceOperation(entityType: EntityType, val application: CrudApplication) extends Operation {
  def invoke(uri: UriPath, persistence: CrudPersistence)

  def invoke(uri: UriPath, activity: ActivityWithVars) {
    application.withEntityPersistence(entityType, activity) { persistence => invoke(uri, persistence) }
  }
}

