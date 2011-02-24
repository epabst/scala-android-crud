package geeks.crud.android

import _root_.android.content.{ContentUris, Intent}
import _root_.android.view.View
import reflect.ClassManifest
import java.util.{Calendar,GregorianCalendar}
import _root_.android.widget.{ArrayAdapter, Spinner, DatePicker, TextView}
import geeks.crud._

/**
 * Field fieldAccess for Views.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/16/11
 * Time: 6:30 AM
 */

abstract class ViewFieldAccess[V <: View,T](implicit m: ClassManifest[V]) extends FieldAccess[V,V,T]

/** View fieldAccess for a View resource within a given parent View */
class ViewFieldAccessById[T](val viewResourceId: Int)(childViewFieldAccess: ViewFieldAccess[_,T])
        extends PartialFieldAccess[T] {
  def partialGet(readable: AnyRef) = readable match {
    case entryView: View => Option(entryView.findViewById(viewResourceId)).flatMap(v => partialGetFromChildView(v))
    case _ => None
  }

  def partialSet(writable: AnyRef, value: T) = writable match {
    case entryView: View => partialSetInChildView(entryView.findViewById(viewResourceId), value)
    case _ => false
  }

  def partialGetFromChildView(childView: View) = childViewFieldAccess.partialGet(childView)

  def partialSetInChildView(childView: View, value: T) = childViewFieldAccess.partialSet(childView, value)
}

object ViewFieldAccess {
  def viewFieldAccess[V <: View,T](getter: V => T, setter: V => T => Unit)(implicit m: ClassManifest[V]): ViewFieldAccess[V,T] = {
    new ViewFieldAccess[V,T] {
      def get(view: V) = getter(view)

      def set(view: V, value: T) = setter(view)(value)
    }
  }

  def viewId[V <: View,T](viewResourceId: Int)(implicit childViewFieldAccess: ViewFieldAccess[V,T]): ViewFieldAccessById[T] = {
    new ViewFieldAccessById[T](viewResourceId)(childViewFieldAccess)
  }

  def viewFieldAccessById[V <: View,T](viewResourceId: Int, getter: V => T, setter: V => T => Unit)
                                 (implicit m: ClassManifest[V]): ViewFieldAccessById[T] = {
    viewId(viewResourceId)(viewFieldAccess(getter,setter))
  }

  def formattedTextViewFieldAccess[T](format: ValueFormat[T]): ViewFieldAccess[TextView,T] =
    //todo deal with v.getText being null and toValue returning None
    viewFieldAccess[TextView,T](v => format.toValue(v.getText.toString).get, v => value => v.setText(format.toString(value)))

  implicit def primitiveTextViewFieldAccess[T <: AnyVal](implicit m: Manifest[T]): ViewFieldAccess[TextView,T] =
    formattedTextViewFieldAccess(new BasicValueFormat[T]())

  implicit val stringTextViewFieldAccess: ViewFieldAccess[TextView,String] = viewFieldAccess[TextView,String](_.getText.toString, _.setText)

  implicit val calendarDatePickerFieldAccess: ViewFieldAccess[DatePicker,Calendar] = viewFieldAccess[DatePicker,Calendar](
    v => new GregorianCalendar(v.getYear, v.getMonth, v.getDayOfMonth),
    v => calendar => v.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)))

  def enumerationSpinnerFieldAccess[E <: Ordered[_]](enum: Enumeration, default: E): ViewFieldAccess[Spinner,E] = {
    val valueArray: Array[E] = enum.values.toArray.asInstanceOf[Array[E]]
    viewFieldAccess[Spinner,E](v => Option(v.getSelectedItem.asInstanceOf[E]).getOrElse(default), spinner => value => {
      if (spinner.getAdapter == null) {
        spinner.setAdapter(new ArrayAdapter[E](spinner.getContext, _root_.android.R.layout.simple_spinner_item, valueArray))
      }
      spinner.setSelection(valueArray.indexOf(value))
    })
  }

  val intentId = Field.readOnly[Intent,Long](intent => ContentUris.parseId(intent.getData))
}