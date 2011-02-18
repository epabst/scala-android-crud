package geeks.crud.android

import android.content.ContentValues
import android.database.Cursor

object CursorField {
  def persisted[T](name: String)(implicit persistedType: PersistedType[T]): CursorAccess[T] = {
    new CursorAccess[T](name)(persistedType)
  }
}

class CursorAccess[T](val name: String)(implicit val persistedType: PersistedType[T]) extends TypeAccess[Cursor,ContentValues,T] {
  def get(cursor: Cursor) = persistedType.getValue(cursor, cursor.getColumnIndex(name))

  def set(contentValues: ContentValues, value: T) = persistedType.putValue(contentValues, name, value)
}
