package com.github.scala_android.crud.persistence

import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import com.github.triangle._
import PortableField._
import android.os.Bundle
import com.github.scala_android.crud.common.{PlatformTypes, Logging}

class SQLiteCriteria(var selection: String = null, var selectionArgs: Array[String] = Nil.toArray,
                     var groupBy: String = null, var having: String = null, var orderBy: String = null)

object CursorField extends PlatformTypes {
  def persisted[T](name: String)(implicit persistedType: PersistedType[T]): CursorField[T] = {
    new CursorField[T](name)(persistedType)
  }

  val persistedId = persisted[ID](BaseColumns._ID)

  def persistedFields(field: BaseField): List[CursorField[_]] = {
    field.deepCollect[CursorField[_]] {
      case cursorField: CursorField[_] => cursorField
    }
  }

  def queryFieldNames(fields: FieldList): List[String] = persistedFields(fields).map(_.name)

  def sqliteCriteria[T](name: String) = PortableField.writeOnlyDirect[SQLiteCriteria,T](criteria => value => criteria.selection = name + "=" + value)
}

/**
 * Also supports accessing a scala Map (mutable.Map for writing) using the same name.
 */
class CursorField[T](val name: String)(implicit val persistedType: PersistedType[T]) extends DelegatingPortableField[T] with Logging {
  protected val delegate =
    readOnly[Cursor,T](getFromCursor) + writeOnlyDirect[ContentValues,T](setIntoContentValues) +
    fieldDirect[Bundle,T](b => persistedType.getValue(b, name), b => v => persistedType.putValue(b, name, v)) +
    mapField[T](name)

  private def getFromCursor(cursor: Cursor) = {
    val columnIndex = cursor.getColumnIndex(name)
    if (columnIndex >= 0) {
      persistedType.getValue(cursor, columnIndex)
    } else {
      warn("column not in Cursor: " + name)
      None
    }
  }

  private def setIntoContentValues(contentValues: ContentValues)(value: T) { persistedType.putValue(contentValues, name, value) }

  override def toString = "persisted(\"" + name + "\")"
}
