package com.github.scala_android.crud

/**
 * An Application that works with {@link CrudEntityType}s.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/31/11
 * Time: 4:50 PM
 */

trait CrudApplication {
  /**
   * All entities in the application, in priority order of most interesting first.
   */
  def allEntities: List[CrudEntityTypeRef]
}