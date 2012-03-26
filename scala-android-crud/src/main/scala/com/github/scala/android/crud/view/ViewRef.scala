package com.github.scala.android.crud.view

import com.github.scala.android.crud.common.PlatformTypes._
import com.github.scala.android.crud.view.AndroidResourceAnalyzer._

/**
 * A reference to an element in the UI.  It wraps a [[com.github.scala.android.crud.common.PlatformTypes.ViewKey]].
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/26/12
 * Time: 6:39 AM
 */

trait ViewRef {
  def viewKeyOpt: Option[ViewKey]

  def viewKeyOrError: ViewKey

  /**
   * The name of the field as a String.
   * @param rIdClasses a list of R.id classes that may contain the id.
   * @throws IllegalStateException if it cannot be determined
   */
  def fieldName(rIdClasses: Seq[Class[_]]): String

  override def toString: String
}

object ViewRef {
  def apply(viewKey: ViewKey): ViewRef = {
    new ViewRef {
      def viewKeyOpt = Some(viewKey)

      def viewKeyOrError = viewKey

      def fieldName(rIdClasses: Seq[Class[_]]): String = {
        resourceFieldWithIntValue(rIdClasses, viewKey).getName
      }

      override def toString = viewKey.toString
    }
  }

  def apply(viewResourceIdName: String, rIdClasses: Seq[Class[_]]): ViewRef = {
    new ViewRef {
      def viewKeyOpt = findResourceIdWithName(rIdClasses, viewResourceIdName)

      def viewKeyOrError = resourceIdWithName(rIdClasses, viewResourceIdName)

      def fieldName(rIdClasses: Seq[Class[_]]): String = viewResourceIdName

      override def toString = viewResourceIdName
    }
  }
}