package geeks.crud.android

import android.view.View
import reflect.ClassManifest
import android.widget.{DatePicker, TextView}
import geeks.crud.{BasicValueFormat, ValueFormat}
import java.util.{Calendar,GregorianCalendar}

/**
 * Field access for Views.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/16/11
 * Time: 6:30 AM
 */

abstract class ViewAccess[V <: View,T](implicit m: ClassManifest[V]) extends TypeAccess[V,V,T]

/** View access for a View resource within a given parent View */
class ViewAccessById[V <: View,T](val viewResourceId: Int)(implicit childViewAccess: ViewAccess[V,T])
        extends ViewAccess[View,T] {
  def get(entryView: View) = getFromChildView(entryView.findViewById(viewResourceId).asInstanceOf[V])

  def set(entryView: View, value: T) = setInChildView(entryView.findViewById(viewResourceId).asInstanceOf[V], value)

  def getFromChildView(childView: V) = childViewAccess.get(childView)

  def setInChildView(childView: V, value: T) = childViewAccess.set(childView, value)
}

object ViewAccess {
  def viewAccess[V <: View,T](getter: V => T, setter: V => T => Unit)(implicit m: ClassManifest[V]): ViewAccess[V,T] = {
    new ViewAccess[V,T] {
      def get(view: V) = getter(view)

      def set(view: V, value: T) = setter(view)(value)
    }
  }

  def viewId[V <: View,T](viewResourceId: Int)(implicit childViewAccess: ViewAccess[V,T]): ViewAccessById[V,T] = {
    new ViewAccessById[V,T](viewResourceId)
  }

  def viewAccessById[V <: View,T](viewResourceId: Int, getter: V => T, setter: V => T => Unit)
                                 (implicit m: ClassManifest[V]): ViewAccessById[V,T] = {
    viewId(viewResourceId)(viewAccess(getter,setter))
  }

  def formattedTextViewAccess[T](format: ValueFormat[T]): ViewAccess[TextView,T] =
    //todo deal with v.getText being null and toValue returning None
    viewAccess[TextView,T](v => format.toValue(v.getText.toString).get, v => value => v.setText(format.toString(value)))

  implicit def primitiveTextViewAccess[T <: AnyVal](implicit m: Manifest[T]): ViewAccess[TextView,T] =
    formattedTextViewAccess(new BasicValueFormat[T]())

  implicit val stringTextViewAccess: ViewAccess[TextView,String] = viewAccess[TextView,String](_.getText.toString, _.setText)

  implicit val calendarDatePickerAccess: ViewAccess[DatePicker,Calendar] = viewAccess[DatePicker,Calendar](
    v => new GregorianCalendar(v.getYear, v.getMonth, v.getDayOfMonth),
    v => calendar => v.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)))
}