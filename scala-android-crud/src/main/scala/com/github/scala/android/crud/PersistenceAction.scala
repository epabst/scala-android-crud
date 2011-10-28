package com.github.scala.android.crud

import action.{ActivityWithVars, UriPath, BaseAction}
import common.PlatformTypes

/**
 * An action that interacts with an entity's persistence.
 * The CrudContext is available as persistence.crudContext to implementing classes.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 10/21/11
 * Time: 6:59 AM
 */
abstract class PersistenceAction(entityType: CrudType, val application: CrudApplication,
                   icon: Option[PlatformTypes#ImgKey], title: Option[PlatformTypes#SKey])
        extends BaseAction(icon, title) {
  def invoke(uri: UriPath, persistence: CrudPersistence)

  def invoke(uri: UriPath, activity: ActivityWithVars) {
    entityType.withEntityPersistence(new CrudContext(activity, application), { persistence => invoke(uri, persistence) })
  }
}

