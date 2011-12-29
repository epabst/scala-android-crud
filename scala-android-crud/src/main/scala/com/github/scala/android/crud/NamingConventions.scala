package com.github.scala.android.crud

/** A utility that defines the naming conventions for Crud applications.
  * @author Eric Pabst (epabst@gmail.com)
  */

object NamingConventions {
  def toLayoutPrefix(entityName: String): String = entityName.collect {
    case c if (c.isUpper) => "_" + c.toLower
    case c if (Character.isJavaIdentifierPart(c)) => c.toString
  }.mkString.stripPrefix("_")
}