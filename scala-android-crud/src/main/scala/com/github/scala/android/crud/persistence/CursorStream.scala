package com.github.scala.android.crud.persistence

import android.database.Cursor
import com.github.triangle.FieldList

/** A Stream that wraps a Cursor.
  * @author Eric Pabst (epabst@gmail.com)
  */
case class CursorStream(cursor: Cursor, persistedFields: List[CursorField[_]]) extends Stream[Map[String,Any]] {
  override lazy val headOption = {
    if (cursor.moveToNext) {
      Some(FieldList(persistedFields: _*).transform(Map.empty[String,Any], cursor))
    } else {
      cursor.close()
      None
    }
  }

  override def isEmpty : scala.Boolean = headOption.isEmpty
  override def head = headOption.get
  def tailDefined = !isEmpty
  // Must be a val so that we don't create more than one CursorStream.
  // Must be lazy so that we don't instantiate the entire stream
  override lazy val tail = if (tailDefined) CursorStream(cursor, persistedFields) else throw new NoSuchElementException
}
