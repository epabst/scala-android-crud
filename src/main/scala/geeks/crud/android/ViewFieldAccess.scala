package geeks.crud.android

import android.view.View
import reflect.ClassManifest
import android.widget.{DatePicker, TextView}
import geeks.crud.{BasicValueFormat, ValueFormat}
import java.util.{Calendar,GregorianCalendar}

/**
 * Field fieldAccess for Views.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/16/11
 * Time: 6:30 AM
 */

abstract class ViewFieldAccess[V <: View,T](implicit m: ClassManifest[V]) extends FieldAccess[V,V,T]

/** View fieldAccess for a View resource within a given parent View */
class ViewFieldAccessById[V <: View,T](val viewResourceId: Int)(implicit childViewFieldAccess: ViewFieldAccess[V,T])
        extends ViewFieldAccess[View,T] {
  def get(entryView: View) = getFromChildView(entryView.findViewById(viewResourceId).asInstanceOf[V])

  def set(entryView: View, value: T) = setInChildView(entryView.findViewById(viewResourceId).asInstanceOf[V], value)

  def getFromChildView(childView: V) = childViewFieldAccess.get(childView)

  def setInChildView(childView: V, value: T) = childViewFieldAccess.set(childView, value)
}

object ViewFieldAccess {
  def viewFieldAccess[V <: View,T](getter: V => T, setter: V => T => Unit)(implicit m: ClassManifest[V]): ViewFieldAccess[V,T] = {
    new ViewFieldAccess[V,T] {
      def get(view: V) = getter(view)

      def set(view: V, value: T) = setter(view)(value)
    }
  }

  def viewId[V <: View,T](viewResourceId: Int)(implicit childViewFieldAccess: ViewFieldAccess[V,T]): ViewFieldAccessById[V,T] = {
    new ViewFieldAccessById[V,T](viewResourceId)
  }

  def viewFieldAccessById[V <: View,T](viewResourceId: Int, getter: V => T, setter: V => T => Unit)
                                 (implicit m: ClassManifest[V]): ViewFieldAccessById[V,T] = {
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
}