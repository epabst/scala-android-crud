package com.github.scala_android.crud.action

import com.github.scala_android.crud.common.PlatformTypes
import collection.JavaConversions
import com.github.triangle.ValueFormat._
import android.net.Uri

/**
 * A convenience wrapper for Uri.
 * It helps in that Uri.EMPTY is null when running unit tests, and helps prepare for multi-platform support.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 9/22/11
 * Time: 10:42 PM
 */
case class UriPath(entityName: String, detail: String*) extends PlatformTypes {
  import JavaConversions._
  private val idFormat = basicFormat[ID]

  def specifyInUri(currentUri: Uri): Uri =
    replacePathSegments(currentUri, _.takeWhile(_ != entityName) ::: entityName :: detail.toList)

  def findId(currentUri: Uri): Option[ID] =
    currentUri.getPathSegments.toList.dropWhile(_ != entityName) match {
      case nameString :: idString :: x => idFormat.toValue(idString)
      case _ => None
    }

  def keepUpToTheIdInUri(currentUri: Uri): Uri =
    UriPath(entityName, findId(currentUri).get.toString).specifyInUri(currentUri)

  private def replacePathSegments(uri: Uri, f: List[String] => List[String]): Uri = {
    val path = f(uri.getPathSegments.toList)
    Action.toUri(path: _*)
  }
}
