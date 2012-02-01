package com.github.scala.android.crud.persistence

import android.database.Cursor
import com.github.triangle.FieldList

case class EntityTypePersistedInfo(persistedFields: List[CursorField[_]]) {
  private val persistedFieldList = FieldList(persistedFields: _*)
  lazy val queryFieldNames: List[String] = persistedFields.map(_.columnName)

  /** Copies the current row of the given cursor to a Map.  This allows the Cursor to then move to a different position right after this. */
  def copyRowToMap(cursor: Cursor): Map[String,Any] =
    persistedFieldList.copyAndTransform(cursor, Map.empty[String,Any])
}

object EntityTypePersistedInfo {
  def apply(entityType: EntityType): EntityTypePersistedInfo = EntityTypePersistedInfo(CursorField.persistedFields(entityType))
}

/** A Stream that wraps a Cursor.
  * @author Eric Pabst (epabst@gmail.com)
  */
case class CursorStream(cursor: Cursor, entityTypePersistedInfo: EntityTypePersistedInfo) extends Stream[Map[String,Any]] {
  override lazy val headOption = {
    if (cursor.moveToNext) {
      Some(entityTypePersistedInfo.copyRowToMap(cursor))
    } else {
      cursor.close()
      None
    }
  }

  override def isEmpty : scala.Boolean = headOption.isEmpty
  override def head = headOption.get
  override def length = cursor.getCount

  def tailDefined = !isEmpty
  // Must be a val so that we don't create more than one CursorStream.
  // Must be lazy so that we don't instantiate the entire stream
  override lazy val tail = if (tailDefined) CursorStream(cursor, entityTypePersistedInfo) else throw new NoSuchElementException
}
