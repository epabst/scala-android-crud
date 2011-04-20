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

  def fieldAccessFlatMap[B](fields: List[CopyableField], f: (PartialFieldAccess[_]) => Traversable[B]): List[B] =
    fields.map(_.asInstanceOf[Field[_]].fieldAccesses).flatMap(_.flatMap(f))

  val persistedId = persisted[ID](BaseColumns._ID)

  def queryFieldNames(fields: List[CopyableField]): List[String] = {
    persistedId.name :: fieldAccessFlatMap(fields, _ match {
      case fieldAccess: CursorFieldAccess[_] => Some(fieldAccess.name)
      case foreignKey: ForeignKey => Some(foreignKey.fieldName)
      case _ => None
    })
  }

  def sqliteCriteria[T](name: String) = Field.writeOnly[SQLiteCriteria,T](criteria => value => criteria.selection = name + "=" + value)
}

/**
 * Also supports accessing a scala Map (mutable.Map for writing) using the same name.
 */
class CursorFieldAccess[T](val name: String)(implicit val persistedType: PersistedType[T]) extends FieldAccessVariations[T] with Logging {
  val fieldAccesses = List(
    Field.flow[Cursor,ContentValues,T](getFromCursor, setIntoContentValues),
    Field.fieldAccess[Bundle,T](b => persistedType.getValue(b, name), b => v => persistedType.putValue(b, name, v)),
    Field.mapAccess[T](name))

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

class ForeignKey(val entityType: CrudEntityTypeRef) extends PlatformTypes with FieldAccessVariations[ID] {
  val fieldName = entityType.entityName.toLowerCase + BaseColumns._ID
  val fieldAccesses = List[PartialFieldAccess[ID]](persisted(fieldName), intentId(entityType.entityName), sqliteCriteria(fieldName))
}
