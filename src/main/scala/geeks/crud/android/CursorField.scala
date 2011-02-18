package geeks.crud.android

import android.content.ContentValues
import android.view.View
import android.widget.{TextView, DatePicker}
import android.database.Cursor
import java.util.{Calendar, GregorianCalendar}
import geeks.crud.{BasicValueFormat, ValueFormat}
import geeks.crud.util.Logging
import geeks.crud.android.ViewAccess._

object CursorField {
  def persisted[T](name: String)(implicit persistedType: PersistedType[T]): CursorAccess[T] = {
    new CursorAccess[T](name)(persistedType)
  }
}

class CursorAccess[T](val name: String)(implicit val persistedType: PersistedType[T]) extends TypeAccess[Cursor,ContentValues,T] {
  def get(cursor: Cursor) = persistedType.getValue(cursor, cursor.getColumnIndex(name))

  def set(contentValues: ContentValues, value: T) = persistedType.putValue(contentValues, name, value)
}

import CursorField._

class StringTextViewField(persistedFieldName: String, viewResourceId: Int) extends Field[String](
  persisted(persistedFieldName), viewId[TextView,String](viewResourceId))

class LongDatePickerField(persistedFieldName: String, viewResourceId: Int) extends Field[Calendar](
  persisted(persistedFieldName)(PersistedType.calendarLongType),
  viewId[DatePicker,Calendar](viewResourceId))

class PrimitiveTextViewField[T <: AnyVal](persistedFieldName: String, viewResourceId: Int)
                                         (implicit persistedType: PersistedType[T], childViewAccess: ViewAccess[TextView,T]) extends Field[T](
  persisted(persistedFieldName),
  viewId[TextView,T](viewResourceId))

class ConvertedTextViewField[T](persistedFieldName: String, viewResourceId: Int)
                               (implicit valueFormat: ValueFormat[T], persistedType: PersistedType[T]) extends Field[T](
  persisted(persistedFieldName),
  viewAccessById[TextView,T](viewResourceId, v => valueFormat.toValue(v.getText.toString).get, v => value => v.setText(valueFormat.toString(value))))

class HiddenField[T](val persistedName: String)(implicit persistedType: PersistedType[T]) extends Field[T](persisted(persistedName))
