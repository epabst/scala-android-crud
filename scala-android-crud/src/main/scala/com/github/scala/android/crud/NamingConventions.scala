package com.github.scala.android.crud

/**
 * A utility that defines the naming conventions for Crud applications.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 9/27/11
 * Time: 6:18 PM
 */

object NamingConventions {
  def toLayoutPrefix(entityName: String): String = entityName.collect {
    case c if (c.isUpper) => "_" + c.toLower
    case c if (Character.isJavaIdentifierPart(c)) => c.toString
  }.mkString.stripPrefix("_")
}