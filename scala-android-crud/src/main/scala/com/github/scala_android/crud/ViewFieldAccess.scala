package com.github.scala_android.crud

import _root_.android.app.Activity
import _root_.android.content.Intent
import _root_.android.view.View
import reflect.ClassManifest
import _root_.android.widget.{ArrayAdapter, Spinner, DatePicker, TextView}
import com.github.triangle._
import Field._
import java.util.{Calendar, Date, GregorianCalendar}

/**
 * Field fieldAccess for Views.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/16/11
 * Time: 6:30 AM
 */

abstract class ViewFieldAccess[V <: View,T](implicit m: ClassManifest[V]) extends FieldAccess[V,V,T]

object ViewFieldAccess extends PlatformTypes {
  private class ChildViewById(viewResourceId: ViewKey) {
    def unapply(target: Any): Option[View] = target match {
      case view: View => Option(view.findViewById(viewResourceId))
      case activity: Activity => Option(activity.findViewById(viewResourceId))
      case _ => None
    }
  }

  /** View fieldAccess for a View resource within a given parent View */
  class ViewFieldAccessById[T](val viewResourceId: ViewKey)(childViewFieldAccess: PartialFieldAccess[T])
          extends PartialFieldAccess[T] {
    private object ChildView extends ChildViewById(viewResourceId)

    def getter = {
      case ChildView(childView) if childViewFieldAccess.getter.isDefinedAt(childView) =>
        childViewFieldAccess.getter(childView)
    }

    def setter = {
      case ChildView(childView) if childViewFieldAccess.setter.isDefinedAt(childView) =>
        childViewFieldAccess.setter(childView)
    }
  }

  def viewFieldAccess[V <: View,T](getter1: V => Option[T], setter1: V => T => Unit, clearer: V => Unit = {_: V => })
                                  (implicit m: ClassManifest[V]): ViewFieldAccess[V,T] = {
    new ViewFieldAccess[V,T] {
      def get(view: V) = getter1(view)

      def set(view: V, optionalValue: Option[T]) {
        optionalValue match {
          case Some(value) => setter1(view)(value)
          case None => clearer(view)
        }
      }
    }
  }

  val textView: ViewFieldAccess[TextView,String] = viewFieldAccess[TextView,String](v => toOption(v.getText.toString.trim), _.setText, _.setText(""))

  def viewId[T](viewResourceId: ViewKey, childViewAccessVariations: PartialFieldAccess[T]*): ViewFieldAccessById[T] = {
    new ViewFieldAccessById[T](viewResourceId)(Field.variations(childViewAccessVariations: _*))
  }

  def viewFieldAccessById[V <: View,T](viewResourceId: ViewKey, getter: V => Option[T], setter: V => T => Unit, clearer: V => Unit = {_: V => })
                                 (implicit m: ClassManifest[V]): ViewFieldAccessById[T] = {
    viewId(viewResourceId, viewFieldAccess(getter, setter, clearer))
  }

  private def toOption(string: String): Option[String] = if (string == "") None else Some(string)

  implicit val datePickerFieldAccess: ViewFieldAccess[DatePicker,Date] = viewFieldAccess[DatePicker,Date](
    v => Some(new GregorianCalendar(v.getYear, v.getMonth, v.getDayOfMonth).getTime),
    v => date => {
      val calendar = new GregorianCalendar()
      calendar.setTime(date)
      v.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
    })

  val calendarDatePicker: ViewFieldAccess[DatePicker,Calendar] = viewFieldAccess[DatePicker,Calendar](
    v => Some(new GregorianCalendar(v.getYear, v.getMonth, v.getDayOfMonth)),
    v => calendar => v.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)))

  def enumerationSpinner[E <: Ordered[_]](enum: Enumeration): ViewFieldAccess[Spinner,E] = {
    val valueArray: Array[E] = enum.values.toArray.asInstanceOf[Array[E]]
    viewFieldAccess[Spinner,E](v => Option(v.getSelectedItem.asInstanceOf[E]), spinner => value => {
      //don't do it again if already done from a previous time
      if (spinner.getAdapter == null) {
        spinner.setAdapter(new ArrayAdapter[E](spinner.getContext, _root_.android.R.layout.simple_spinner_item, valueArray))
      }
      spinner.setSelection(valueArray.indexOf(value))
    })
  }

  def intentId(entityName: String): FieldGetter[Intent,ID] = {
    val uriSegment = EntityUriSegment(entityName)
    Field.readOnly[Intent,ID](intent => uriSegment.findId(intent.getData))
  }
}