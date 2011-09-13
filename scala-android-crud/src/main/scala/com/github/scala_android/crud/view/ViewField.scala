package com.github.scala_android.crud.view

import _root_.android.app.Activity
import _root_.android.content.Intent
import _root_.android.view.View
import _root_.android.widget.{ArrayAdapter, Spinner, DatePicker, TextView}
import com.github.scala_android.crud.action.EntityUriSegment
import com.github.scala_android.crud.common.PlatformTypes
import com.github.triangle._
import PortableField._
import java.util.{Calendar, Date, GregorianCalendar}
import android.net.Uri
import FieldLayout._
import ValueFormat._

/**
 * PortableField for Views.
 * @param defaultLayout the default layout used as an example and by {@link CrudUIGenerator}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/16/11
 * Time: 6:30 AM
 */
abstract class ViewField[T](val defaultLayout: FieldLayout) extends DelegatingPortableField[T]

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
          extends FieldWithDelegate[T] with TransformerUsingSetter[T] {
    private object ChildView extends ChildViewById(viewResourceId)

    protected def delegate = childViewField

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

  val textView: ViewField[String] = new ViewField[String](FieldLayout.nameLayout) {
    protected def delegate = fieldDirect[TextView,String](v => toOption(v.getText.toString.trim), v => v.setText(_), _.setText(""))

    override def toString = "textView"
  }
  lazy val currencyView: PortableField[Double] = formatted(currencyValueFormat, textView)
  lazy val intView: PortableField[Int] = formatted[Int](textView)

  def viewId[T](viewResourceId: ViewKey, childViewField: PortableField[T]): ViewIdField[T] = {
    new ViewIdField[T](viewResourceId, childViewField)
  }

  private def toOption(string: String): Option[String] = if (string == "") None else Some(string)

  val calendarDateView: PortableField[Calendar] = new ViewField[Calendar](datePickerLayout) {
    protected def delegate = formatted(calendarValueFormat, textView) + fieldDirect[DatePicker,Calendar](
      v => Some(new GregorianCalendar(v.getYear, v.getMonth, v.getDayOfMonth)),
      v => calendar => v.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)))

    override def toString = "calendarDateView"
  }

  implicit val dateView: PortableField[Date] = new ConvertedField[Date,Calendar](calendarDateView) {
    def convert(calendar: Calendar) = calendar.getTime

    def unconvert(date: Date) = {
      val calendar = new GregorianCalendar()
      calendar.setTime(date)
      calendar
    }

    override def toString = "dateView"
  }

  def enumerationView[E <: Enumeration#Value](enum: Enumeration): PortableField[E] = {
    val valueArray: Array[E] = enum.values.toArray.asInstanceOf[Array[E]]
    val defaultLayout = new FieldLayout {
      def displayXml = <TextView/>
      def editXml = <Spinner android:drawSelectorOnTop = "true"/>
    }
    new ViewField[E](defaultLayout) {
      private val spinnerField: PortableField[E] = fieldDirect[Spinner,E](v => Option(v.getSelectedItem.asInstanceOf[E]), spinner => value => {
        //don't do it again if already done from a previous time
        if (spinner.getAdapter == null) {
          spinner.setAdapter(new ArrayAdapter[E](spinner.getContext, _root_.android.R.layout.simple_spinner_item, valueArray))
        }
        spinner.setSelection(valueArray.indexOf(value))
      })
      protected def delegate = spinnerField + formatted[E](enumFormat(enum), textView)
    }
  }

  def intentId(entityName: String): FieldGetter[Intent,ID] = {
    val uriSegment = EntityUriSegment(entityName)
    PortableField.readOnly[Intent,ID](intent => uriSegment.findId(intent.getData))
  }

  def uriId(entityName: String): FieldGetter[Uri,ID] = {
    val uriSegment = EntityUriSegment(entityName)
    PortableField.readOnly[Uri,ID](uri => uriSegment.findId(uri))
  }
}