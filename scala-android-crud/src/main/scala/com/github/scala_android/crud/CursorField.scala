package com.github.scala_android.crud

import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import com.github.triangle._
import android.os.Bundle
import monitor.Logging

object CursorField extends PlatformTypes {
  def persisted[T](name: String)(implicit persistedType: PersistedType[T]): CursorField[T] = {
    new CursorField[T](name)(persistedType)
  }

  def foreignKey[ID](entityType: CrudTypeRef) = new ForeignKey(entityType)

  val persistedId = persisted[ID](BaseColumns._ID)

  def persistedFields(fields: FieldList): List[CursorField[_]] = {
    persistedId :: fields.fieldFlatMap[CursorField[_]] {
      case cursorField: CursorField[_] => List(cursorField)
    }
  }

  def queryFieldNames(fields: FieldList): List[String] = persistedFields(fields).map(_.name)

  def sqliteCriteria[T](name: String) = PortableField.writeOnly[SQLiteCriteria,T](criteria => value => criteria.selection = name + "=" + value)
}

/**
 * Also supports accessing a scala Map (mutable.Map for writing) using the same name.
 */
class CursorField[T](val name: String)(implicit val persistedType: PersistedType[T]) extends DelegatingPortableField[T] with Logging {
  protected val delegate =
    PortableField.flow[Cursor,ContentValues,T](getFromCursor, setIntoContentValues) +
    PortableField.field[Bundle,T](b => persistedType.getValue(b, name), b => v => persistedType.putValue(b, name, v)) +
    PortableField.mapField[T](name)

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

import CursorField._
import ViewField.intentId

class ForeignKey(val entityType: CrudTypeRef) extends PlatformTypes with DelegatingPortableField[ID] {
  val fieldName = entityType.entityName.toLowerCase + BaseColumns._ID

  protected val delegate = persisted[ID](fieldName) + intentId(entityType.entityName) + sqliteCriteria[ID](fieldName)

  override def toString = "foreignKey(" + entityType.getClass.getSimpleName + ")"
}
