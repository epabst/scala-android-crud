package geeks.crud.android

import android.content.ContentValues
import android.view.View
import android.widget.{TextView, DatePicker}
import android.database.Cursor
import java.util.{Calendar, GregorianCalendar}
import geeks.crud.{BasicValueFormat, ValueFormat}
import geeks.crud.util.Logging

object CursorField {
  class CursorAccess[V](val name: String)(implicit val persistedType: PersistedType[V]) extends TypeAccess[Cursor,ContentValues,V] {
    def get(cursor: Cursor) = persistedType.getValue(cursor, cursor.getColumnIndex(name))

    def set(contentValues: ContentValues, value: V) = persistedType.putValue(contentValues, name, value)
  }

  def persisted[V](name: String)(implicit persistedType: PersistedType[V]): CursorAccess[V] = {
    new CursorAccess[V](name)(persistedType)
  }
}

/**
 * A Field that works with Cursor and ContentValues.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/12/11
 * Time: 10:39 PM
 */
trait CursorField extends Field[Cursor,ContentValues]

/**
 * A field that maps directly between View to Cursor column.
 * @param T the persisted type as found in a Cursor or put into ContentValues
 * @param cursorGetter gets the right kind of data from a Cursor
 * @param viewBinder a function that fills the view using a cursor.  For example, _.setText(_.getString)
 */
abstract class DirectField[T,V <: View](persistedFieldName: String, val viewResourceId: Int)
                                       (implicit persistedType: PersistedType[T])
        extends CursorField with ViewField[Cursor,ContentValues] with Logging {
  def queryFieldNames = List(persistedFieldName)

  def getViewValue(view: V): Option[T]

  def setViewValue(view: V, value: T)

  def writeFromFieldView(fieldView: View, contentValues: ContentValues) {
    //does a best-effort, doing nothing if unable to parse value
    getViewValue(fieldView.asInstanceOf[V]).map(value => persistedType.putValue(contentValues, persistedFieldName, value))
  }

  def readIntoFieldView(cursor: Cursor, fieldView: View) {
    val value = persistedType.getValue(cursor, cursor.getColumnIndex(persistedFieldName))
    debug("Setting " + persistedFieldName + " to " + value + " in " + fieldView + " from " + cursor)
    setViewValue(fieldView.asInstanceOf[V], value)
  }
}

/**
 * @param T the persisted type as found in a Cursor or put into ContentValues
 * @param cursorGetter gets the right kind of data from a Cursor
 * @param viewBinder a function that fills the view using a cursor.  For example, _.setText(_.getString)
 */
class SimpleField[T,V <: View](persistedFieldName: String, viewResourceId: Int, viewExtractor: V => T,
                               viewBinder: V => T => Unit)(implicit persistedType: PersistedType[T])
        extends DirectField[T,V](persistedFieldName, viewResourceId)(persistedType) {
  def setViewValue(view: V, value: T) { viewBinder(view)(value) }

  def getViewValue(view: V) = Option(viewExtractor(view))
}

class StringTextViewField(persistedFieldName: String, viewResourceId: Int)
        extends SimpleField[String,TextView](persistedFieldName, viewResourceId, _.getText.toString, _.setText)

class LongDatePickerField(persistedFieldName: String, viewResourceId: Int)
        extends DirectField[Long,DatePicker](persistedFieldName, viewResourceId) {
  def setViewValue(view: DatePicker, value: Long) {
    val calendar = new GregorianCalendar()
    calendar.setTimeInMillis(value)
    view.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
  }

  def getViewValue(view: DatePicker): Option[Long] = {
    Some(new GregorianCalendar(view.getYear, view.getMonth, view.getDayOfMonth).getTimeInMillis)
  }
}

class PrimitiveTextViewField[T](persistedFieldName: String, viewResourceId: Int)
                               (implicit persistedType: PersistedType[T])
        extends DirectField[T,TextView](persistedFieldName, viewResourceId) {
  val valueFormat: ValueFormat[T] = new BasicValueFormat[T]()(persistedType.valueManifest)

  def setViewValue(view: TextView, value: T) { view.setText(valueFormat.toString(value)) }

  def getViewValue(view: TextView) = valueFormat.toValue(view.getText.toString)
}

class ConvertedTextViewField[T](persistedFieldName: String, viewResourceId: Int)
                               (implicit valueFormat: ValueFormat[T], persistedType: PersistedType[T])
        extends DirectField[T,TextView](persistedFieldName, viewResourceId) {
  def setViewValue(view: TextView, value: T) { view.setText(valueFormat.toString(value)) }

  def getViewValue(view: TextView) = valueFormat.toValue(view.getText.toString)
}

class HiddenField[V](val persistedName: String) extends CursorField {
  def queryFieldNames = List(persistedName)

  def writeFromView(entryView: View, contentvalue: ContentValues) {}

  def readIntoView(cursor: Cursor, entryView: View) {}
}
