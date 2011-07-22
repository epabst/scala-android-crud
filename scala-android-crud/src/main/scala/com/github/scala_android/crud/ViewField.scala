package com.github.scala_android.crud

import _root_.android.app.Activity
import _root_.android.content.Intent
import _root_.android.view.View
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
//todo run all UI read/write operations on the UI thread!!!
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

  val textView: PortableField[String] = fieldDirect[TextView,String](v => toOption(v.getText.toString.trim), _.setText, _.setText(""))

  def viewId[T](viewResourceId: ViewKey, childViewField: PortableField[T]): ViewIdField[T] = {
    new ViewIdField[T](viewResourceId, childViewField)
  }

  private def toOption(string: String): Option[String] = if (string == "") None else Some(string)

  implicit val datePicker: PortableField[Date] = fieldDirect[DatePicker,Date](
    v => Some(new GregorianCalendar(v.getYear, v.getMonth, v.getDayOfMonth).getTime),
    v => date => {
      val calendar = new GregorianCalendar()
      calendar.setTime(date)
      v.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
    })

  val calendarDatePicker: PortableField[Calendar] = fieldDirect[DatePicker,Calendar](
    v => Some(new GregorianCalendar(v.getYear, v.getMonth, v.getDayOfMonth)),
    v => calendar => v.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)))

  def enumerationSpinner[E <: Ordered[_]](enum: Enumeration): PortableField[E] = {
    val valueArray: Array[E] = enum.values.toArray.asInstanceOf[Array[E]]
    fieldDirect[Spinner,E](v => Option(v.getSelectedItem.asInstanceOf[E]), spinner => value => {
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