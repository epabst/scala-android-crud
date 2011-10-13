package com.github.scala.android.crud

import com.github.triangle.Logging

/**
 * An Application that works with [[com.github.scala.android.crud.CrudType]]s.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/31/11
 * Time: 4:50 PM
 */

trait CrudApplication extends Logging {
  trace("Instantiated CrudApplication: " + this)

  def name: String

  //this will be used for programmatic uses such as a database name
  lazy val nameId: String = name.replace(" ", "_").toLowerCase

  def classNamePrefix: String = getClass.getSimpleName.replace("$", "").stripSuffix("Application")
  def packageName: String = getClass.getPackage.getName

  /**
   * All entities in the application, in priority order of most interesting first.
   */
  def allEntities: List[CrudType]
}