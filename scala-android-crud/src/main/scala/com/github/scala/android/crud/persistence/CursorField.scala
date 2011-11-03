package com.github.scala.android.crud.persistence

import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import com.github.triangle._
import PortableField._
import android.os.Bundle
import com.github.scala.android.crud.common.PlatformTypes
import com.github.scala.android.crud.ParentField
import com.github.triangle.Converter._

case class SQLiteCriteria(selection: List[String] = Nil, selectionArgs: List[String] = Nil,
                          groupBy: Option[String] = None, having: Option[String] = None, orderBy: Option[String] = None)

object CursorField extends PlatformTypes {
  def bundleField[T](name: String)(implicit persistedType: PersistedType[T]) =
    fieldDirect[Bundle,T](b => persistedType.getValue(b, name), b => v => persistedType.putValue(b, name, v)) +
    mapField[T](name)

  def persisted[T](name: String)(implicit persistedType: PersistedType[T]): CursorField[T] = {
    new CursorField[T](name)(persistedType)
  }

  def persistedEnum[E <: Enumeration#Value](name: String, enumeration: Enumeration)(implicit m: Manifest[E]): PortableField[E] =
    formatted[E](ValueFormat.enumFormat(enumeration), persisted(name))

  def persistedDate(name: String) = converted(dateToLong, longToDate, persisted[Long](name))

  def persistedCalendar(name: String) = converted(calendarToDate, dateToCalendar, persistedDate(name))

  val idFieldName = BaseColumns._ID

  object PersistedId extends Field(persisted[ID](idFieldName) + sqliteCriteria(idFieldName))

  def persistedFields(field: BaseField): List[CursorField[_]] = {
    field.deepCollect[CursorField[_]] {
      case cursorField: CursorField[_] => cursorField
    }
  }

  def updateablePersistedFields(field: BaseField, rIdClasses: Seq[Class[_]]): List[CursorField[_]] = {
    val parentFieldNames = ParentField.parentFields(field).map(_.fieldName)
    persistedFields(field).filterNot(_.name == idFieldName).filterNot(parentFieldNames.contains(_))
  }

  def queryFieldNames(fields: FieldList): List[String] = persistedFields(fields).map(_.columnName)

  def sqliteCriteria[T](name: String): PortableField[T] =
    PortableField.transformOnlyDirect[SQLiteCriteria,T](criteria => v => criteria.copy(selection = (name + "=" + v) +: criteria.selection), c => c)
}

import CursorField._

/**
 * Also supports accessing a scala Map (mutable.Map for writing) using the same name.
 */
class CursorField[T](val name: String)(implicit val persistedType: PersistedType[T]) extends DelegatingPortableField[T] with Logging {
  protected val delegate =
    readOnly[Cursor,T](getFromCursor) + writeOnlyDirect[ContentValues,T](setIntoContentValues) +
    bundleField[T](name)

  lazy val columnName = SQLiteUtil.toNonReservedWord(name)

  private def getFromCursor(cursor: Cursor) = {
    val columnIndex = cursor.getColumnIndex(columnName)
    if (columnIndex >= 0) {
      persistedType.getValue(cursor, columnIndex)
    } else {
      warn("column not in Cursor: " + columnName)
      None
    }
  }

  private def setIntoContentValues(contentValues: ContentValues)(value: T) { persistedType.putValue(contentValues, columnName, value) }

  override def toString = "persisted(\"" + name + "\")"
}

object SQLiteUtil {
  def toNonReservedWord(name: String): String =  name.toUpperCase match {
    case "ABORT" | "ACTION" | "ADD" | "AFTER" | "ALL" | "ALTER" | "ANALYZE" | "AND" | "AS" | "ASC" | "ATTACH" |
      "AUTOINCREMENT" | "BEFORE" | "BEGIN" | "BETWEEN" | "BY" | "CASCADE" | "CASE" | "CAST" | "CHECK" |
      "COLLATE" | "COLUMN" | "COMMIT" | "CONFLICT" | "CONSTRAINT" | "CREATE" | "CROSS" | "CURRENT_DATE" |
      "CURRENT_TIME" | "CURRENT_TIMESTAMP" | "DATABASE" | "DEFAULT" | "DEFERRABLE" | "DEFERRED" | "DELETE" |
      "DESC" | "DETACH" | "DISTINCT" | "DROP" | "EACH" | "ELSE" | "END" | "ESCAPE" | "EXCEPT" | "EXCLUSIVE" |
      "EXISTS" | "EXPLAIN" | "FAIL" | "FOR" | "FOREIGN" | "FROM" | "FULL" | "GLOB" | "GROUP" | "HAVING" |
      "IF" | "IGNORE" | "IMMEDIATE" | "IN" | "INDEX" | "INDEXED" | "INITIALLY" | "INNER" | "INSERT" | "INSTEAD" |
      "INTERSECT" | "INTO" | "IS" | "ISNULL" | "JOIN" | "KEY" | "LEFT" | "LIKE" | "LIMIT" | "MATCH" | "NATURAL" |
      "NO" | "NOT" | "NOTNULL" | "NULL" | "OF" | "OFFSET" | "ON" | "OR" | "ORDER" | "OUTER" | "PLAN" |
      "PRAGMA" | "PRIMARY" | "QUERY" | "RAISE" | "REFERENCES" | "REGEXP" | "REINDEX" | "RELEASE" | "RENAME" |
      "REPLACE" | "RESTRICT" | "RIGHT" | "ROLLBACK" | "ROW" | "SAVEPOINT" | "SELECT" | "SET" | "TABLE" |
      "TEMP" | "TEMPORARY" | "THEN" | "TO" | "TRANSACTION" | "TRIGGER" | "UNION" | "UNIQUE" | "UPDATE" |
      "USING" | "VACUUM" | "VALUES" | "VIEW" | "VIRTUAL" | "WHEN" | "WHERE" => name + "0"
    case _ => name
  }
}
