package com.github.scala_android.crud

/**
 * An Application that works with {@link CrudType}s.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/31/11
 * Time: 4:50 PM
 */

trait CrudApplication {
  def name: String

  //this will be used for programmatic uses such as a database name
  lazy val nameId = name.replace(" ", "_").toLowerCase

  /**
   * All entities in the application, in priority order of most interesting first.
   */
  def allEntities: List[CrudTypeRef]
}