package com.github.scala_android.crud

import _root_.android.app.Activity
import _root_.android.content.Intent
import _root_.android.view.View
import reflect.ClassManifest
import _root_.android.widget.{ArrayAdapter, Spinner, DatePicker, TextView}
import com.github.triangle._
import PortableField._
import java.util.{Calendar, Date, GregorianCalendar}

/**
 * PortableField for Views.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/16/11
 * Time: 6:30 AM
 */

abstract class ViewField[V <: View,T](implicit m: ClassManifest[V]) extends FlowField[V,V,T]

object ViewField extends PlatformTypes {
  private class ChildViewById(viewResourceId: ViewKey) {
    def unapply(target: Any): Option[View] = target match {
      case view: View => Option(view.findViewById(viewResourceId))
      case activity: Activity => Option(activity.findViewById(viewResourceId))
      case _ => None
    }
  }

  /** PortableField for a View resource within a given parent View */
  class ViewIdField[T](val viewResourceId: ViewKey, childViewField: PortableField[T])
          extends PortableField[T] with TransformerUsingSetter[T] {
    private object ChildView extends ChildViewById(viewResourceId)

    def getter = {
      case ChildView(childView) if childViewField.getter.isDefinedAt(childView) =>
        childViewField.getter(childView)
    }

    def setter = {
      case ChildView(childView) if childViewField.setter.isDefinedAt(childView) =>
        childViewField.setter(childView)
    }

    override def toString = "viewId(" + viewResourceId + ", " + childViewField + ")"
  }

  def viewField[V <: View,T](getter1: V => Option[T], setter1: V => T => Unit, clearer: V => Unit = {_: V => })
                                  (implicit m: ClassManifest[V]): ViewField[V,T] = {
    new ViewField[V,T] {
      def get(view: V) = getter1(view)

      def set(view: V, optionalValue: Option[T]) {
        optionalValue match {
          case Some(value) => setter1(view)(value)
          case None => clearer(view)
        }
      }

      override def toString = "viewField[" + m.erasure.getName + "]"
    }
  }

  val textView: ViewField[TextView,String] = viewField[TextView,String](v => toOption(v.getText.toString.trim), _.setText, _.setText(""))

  def viewId[T](viewResourceId: ViewKey, childViewField: PortableField[T]): ViewIdField[T] = {
    new ViewIdField[T](viewResourceId, childViewField)
  }

  private def toOption(string: String): Option[String] = if (string == "") None else Some(string)

  implicit val datePicker: ViewField[DatePicker,Date] = viewField[DatePicker,Date](
    v => Some(new GregorianCalendar(v.getYear, v.getMonth, v.getDayOfMonth).getTime),
    v => date => {
      val calendar = new GregorianCalendar()
      calendar.setTime(date)
      v.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
    })

  val calendarDatePicker: ViewField[DatePicker,Calendar] = viewField[DatePicker,Calendar](
    v => Some(new GregorianCalendar(v.getYear, v.getMonth, v.getDayOfMonth)),
    v => calendar => v.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)))

  def enumerationSpinner[E <: Ordered[_]](enum: Enumeration): ViewField[Spinner,E] = {
    val valueArray: Array[E] = enum.values.toArray.asInstanceOf[Array[E]]
    viewField[Spinner,E](v => Option(v.getSelectedItem.asInstanceOf[E]), spinner => value => {
      //don't do it again if already done from a previous time
      if (spinner.getAdapter == null) {
        spinner.setAdapter(new ArrayAdapter[E](spinner.getContext, _root_.android.R.layout.simple_spinner_item, valueArray))
      }
      spinner.setSelection(valueArray.indexOf(value))
    })
  }

  def intentId(entityName: String): FieldGetter[Intent,ID] = {
    val uriSegment = EntityUriSegment(entityName)
    PortableField.readOnly[Intent,ID](intent => uriSegment.findId(intent.getData))
  }
}