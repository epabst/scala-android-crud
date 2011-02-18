package geeks.crud.android

import android.content.ContentValues
import android.database.Cursor

object CursorFieldAccess {
  def persisted[T](name: String)(implicit persistedType: PersistedType[T]): CursorFieldAccess[T] = {
    new CursorFieldAccess[T](name)(persistedType)
  }
}

class CursorFieldAccess[T](val name: String)(implicit val persistedType: PersistedType[T]) extends FieldAccess[Cursor,ContentValues,T] {
  def get(cursor: Cursor) = persistedType.getValue(cursor, cursor.getColumnIndex(name))

  def set(contentValues: ContentValues, value: T) = persistedType.putValue(contentValues, name, value)
}
