package com.github.scala.android.crud.common

import PlatformTypes._
import com.github.triangle.ValueFormat._
import com.github.triangle.{Getter, FieldGetter}

/**
 * A convenience wrapper for UriPath.
 * It helps in that UriPath.EMPTY is null when running unit tests, and helps prepare for multi-platform support.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 9/22/11
 * Time: 10:42 PM
 */
case class UriPath(segments: String*) {
  private lazy val idFormat = basicFormat[ID]

  def /(segment: String): UriPath = UriPath(segments :+ segment:_*)

  def /(id: ID): UriPath = this / idFormat.toString(id)

  def specify(finalSegments: String*): UriPath =
    UriPath.replacePathSegments(this, _.takeWhile(_ != finalSegments.head) ++ finalSegments.toList)

  def findId(entityName: String): Option[ID] =
    segments.dropWhile(_ != entityName).toList match {
      case _ :: idString :: tail => idFormat.toValue(idString)
      case _ => None
    }

  def upToIdOf(entityName: String): UriPath = specify(entityName +: findId(entityName).map(_.toString).toList:_*)

  override def toString = segments.mkString("/", "/", "")
}

object UriPath {
  val EMPTY: UriPath = UriPath()

  private def toOption(string: String): Option[String] = if (string == "") None else Some(string)

  def apply(string: String): UriPath = UriPath(toOption(string.stripPrefix("/")).map(_.split("/").toSeq).getOrElse(Nil):_*)

  private[UriPath] def replacePathSegments(uri: UriPath, f: Seq[String] => Seq[String]): UriPath = {
    val path = f(uri.segments)
    UriPath(path: _*)
  }

  def uriIdField(entityName: String): FieldGetter[UriPath,ID] = Getter[UriPath,ID](_.findId(entityName))
}
