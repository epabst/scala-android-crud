package com.github.scala.android.crud

import action.{ContextVars, ContextWithVars}
import com.github.triangle.Logging
import common.Common
import persistence.EntityType
import java.util.NoSuchElementException

/**
 * An Application that works with [[com.github.scala.android.crud.CrudType]]s.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/31/11
 * Time: 4:50 PM
 */

trait CrudApplication extends Logging {
  def logTag = Common.tryToEvaluate(nameId).getOrElse(Common.logTag)

  trace("Instantiated CrudApplication: " + this)

  def name: String

  //this will be used for programmatic uses such as a database name
  lazy val nameId: String = name.replace(" ", "_").toLowerCase

  def classNamePrefix: String = getClass.getSimpleName.replace("$", "").stripSuffix("Application")
  def packageName: String = getClass.getPackage.getName

  /**
   * All entities in the application, in priority order of most interesting first.
   */
  def allCrudTypes: List[CrudType]

  def crudType(entityType: EntityType): CrudType =
    allCrudTypes.find(_.entityType == entityType).getOrElse(throw new NoSuchElementException(entityType + " not found"))
}

/**
 * A context which can store data for the duration of a single Activity.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 4/2/11
 * Time: 3:43 PM
 */

case class CrudContext(context: ContextWithVars, application: CrudApplication) {
  def vars: ContextVars = context

  def openEntityPersistence(entityType: EntityType): CrudPersistence =
    application.crudType(entityType).openEntityPersistence(this)

  def withEntityPersistence[T](entityType: EntityType)(f: CrudPersistence => T): T =
    application.crudType(entityType).withEntityPersistence(this)(f)
}
