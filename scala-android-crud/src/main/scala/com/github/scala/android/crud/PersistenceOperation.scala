package com.github.scala.android.crud

import action.{Operation, ActivityWithVars}
import common.UriPath

/** An operation that interacts with an entity's persistence.
  * The CrudContext is available as persistence.crudContext to implementing classes.
  * @author Eric Pabst (epabst@gmail.com)
  */
abstract class PersistenceOperation(entityType: CrudType, val application: CrudApplication) extends Operation {
  def invoke(uri: UriPath, persistence: CrudPersistence)

  def invoke(uri: UriPath, activity: ActivityWithVars) {
    entityType.withEntityPersistence(new CrudContext(activity, application)) { persistence => invoke(uri, persistence) }
  }
}

