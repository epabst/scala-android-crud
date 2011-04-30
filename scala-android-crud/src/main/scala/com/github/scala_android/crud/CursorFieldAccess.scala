package com.github.scala_android.crud

import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import com.github.triangle._
import android.os.Bundle
import monitor.Logging

object CursorFieldAccess extends PlatformTypes {
  def persisted[T](name: String)(implicit persistedType: PersistedType[T]): CursorFieldAccess[T] = {
    new CursorFieldAccess[T](name)(persistedType)
  }

  def foreignKey[ID](entityType: CrudEntityTypeRef) = new ForeignKey(entityType)

  val persistedId = persisted[ID](BaseColumns._ID)

  def persistedFields(fields: FieldList): List[CursorFieldAccess[_]] = {
    persistedId :: fields.fieldAccessFlatMap[CursorFieldAccess[_]] {
      case fieldAccess: CursorFieldAccess[_] => List(fieldAccess)
    }
  }

  def queryFieldNames(fields: FieldList): List[String] = persistedFields(fields).map(_.name)

  def sqliteCriteria[T](name: String) = Field.writeOnly[SQLiteCriteria,T](criteria => value => criteria.selection = name + "=" + value)
}

/**
 * Also supports accessing a scala Map (mutable.Map for writing) using the same name.
 */
class CursorFieldAccess[T](val name: String)(implicit val persistedType: PersistedType[T]) extends PartialFieldAccess[T] with Logging {
  private val access =
    Field.flow[Cursor,ContentValues,T](getFromCursor, setIntoContentValues) +
    Field.fieldAccess[Bundle,T](b => persistedType.getValue(b, name), b => v => persistedType.putValue(b, name, v)) +
    Field.mapAccess[T](name)

  def getter = access.getter

  def setter = access.setter

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
}

import CursorFieldAccess._
import ViewFieldAccess.intentId

class ForeignKey(val entityType: CrudEntityTypeRef) extends PlatformTypes with PartialFieldAccess[ID] {
  val fieldName = entityType.entityName.toLowerCase + BaseColumns._ID
  private val access = persisted[ID](fieldName) + intentId(entityType.entityName) + sqliteCriteria[ID](fieldName)

  def getter = access.getter

  def setter = access.setter
}
