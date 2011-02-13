package geeks.crud.android

import android.view.View
import geeks.crud.{BasicValueFormat, ValueFormat}
import geeks.crud.util.Logging
import android.widget.SimpleCursorAdapter.ViewBinder
import android.widget.{DatePicker, SimpleCursorAdapter, TextView, EditText}
import java.util.{Calendar, Date, GregorianCalendar}
import PersistedType._

/**
 * A Field that may be editable in a View and/or persisted.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/4/11
 * Time: 9:25 PM
 * @param R the type being read from such as Cursor
 * @param W the type being written to such as ContentValues
 */
trait Field[R,W] {
  def queryFieldNames: List[String]

  def readIntoView(readable: R, entryView: View)

  def writeFromView(entryView: View, writable: W)
}

/** The base trait of all displayed fields. If it doesn't extend ViewField it will not be displayed by default. */
trait ViewField[R,W] extends Field[R,W] {
  def viewResourceId: Int

  def readIntoFieldView(readable: R, fieldView: View)

  def writeFromFieldView(fieldView: View, writable: W)

  final def readIntoView(readable: R, entryView: View) {
    readIntoFieldView(readable, entryView.findViewById(viewResourceId))
  }

  final def writeFromView(entryView: View, writable: W) {
    writeFromFieldView(entryView.findViewById(viewResourceId), writable)
  }
}
