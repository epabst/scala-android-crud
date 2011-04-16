package com.github.scala_android.crud

import _root_.android.app.Activity
import _root_.android.content.Intent
import _root_.android.view.View
import reflect.ClassManifest
import _root_.android.widget.{ArrayAdapter, Spinner, DatePicker, TextView}
import com.github.triangle._
import java.util.{Calendar, GregorianCalendar}

/**
 * Field fieldAccess for Views.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/16/11
 * Time: 6:30 AM
 */

abstract class ViewFieldAccess[V <: View,T](implicit m: ClassManifest[V]) extends FieldAccess[V,V,T]

object ViewFieldAccess extends PlatformTypes {
  /** View fieldAccess for a View resource within a given parent View */
  class ViewFieldAccessById[T](val viewResourceId: ViewKey)(childViewFieldAccess: PartialFieldAccess[T])
          extends PartialFieldAccess[T] {
    def partialGet(readable: AnyRef) = readable match {
      case entryView: View => Option(entryView.findViewById(viewResourceId)).flatMap(v => partialGetFromChildView(v))
      case activity: Activity => Option(activity.findViewById(viewResourceId)).flatMap(v => partialGetFromChildView(v))
      case _ => None
    }

    def partialSet(writable: AnyRef, value: Option[T]) = writable match {
      case entryView: View => partialSetInChildView(entryView.findViewById(viewResourceId), value)
      case activity: Activity => partialSetInChildView(activity.findViewById(viewResourceId), value)
      case _ => false
    }

    def partialGetFromChildView(childView: View) =
      if (childView != null) childViewFieldAccess.partialGet(childView) else None

    def partialSetInChildView(childView: View, value: Option[T]) =
      if (childView != null) childViewFieldAccess.partialSet(childView, value) else false
  }

  def viewFieldAccess[V <: View,T](getter: V => Option[T], setter: V => T => Unit, clearer: V => Unit = {_: V => })
                                  (implicit m: ClassManifest[V]): ViewFieldAccess[V,T] = {
    new ViewFieldAccess[V,T] {
      def get(view: V) = getter(view)

      //todo test that this calls clearer
      def set(view: V, value: Option[T]) { setter(view)(value.get) }
    }
  }

  def viewId[V <: View,T](viewResourceId: ViewKey)(implicit childViewFieldAccess: ViewFieldAccess[V,T]): ViewFieldAccessById[T] = {
    new ViewFieldAccessById[T](viewResourceId)(childViewFieldAccess)
  }

  def viewId[T](viewResourceId: ViewKey, childViewAccessVariations: ViewFieldAccess[_,T]*): ViewFieldAccessById[T] = {
    new ViewFieldAccessById[T](viewResourceId)(Field.variations(childViewAccessVariations: _*))
  }

  def viewFieldAccessById[V <: View,T](viewResourceId: ViewKey, getter: V => Option[T], setter: V => T => Unit, clearer: V => Unit = {_: V => })
                                 (implicit m: ClassManifest[V]): ViewFieldAccessById[T] = {
    viewId(viewResourceId)(viewFieldAccess(getter,setter, clearer))
  }

  def formattedTextViewFieldAccess[T](format: ValueFormat[T]): ViewFieldAccess[TextView,T] =
//todo test that this clears the TextView
    viewFieldAccess[TextView,T](v => format.toValue(v.getText.toString), v => value => v.setText(format.toString(value)))

  implicit def primitiveTextViewFieldAccess[T <: AnyVal](implicit m: Manifest[T]): ViewFieldAccess[TextView,T] =
    formattedTextViewFieldAccess(new BasicValueFormat[T]())

  implicit val stringTextViewFieldAccess: ViewFieldAccess[TextView,String] = viewFieldAccess[TextView,String](v => toOption(v.getText.toString.trim), _.setText)

  private def toOption(string: String): Option[String] = if (string == "") None else Some(string)

  implicit val calendarDatePickerFieldAccess: ViewFieldAccess[DatePicker,Calendar] = viewFieldAccess[DatePicker,Calendar](
//todo test that this clears the DatePicker
    v => Some(new GregorianCalendar(v.getYear, v.getMonth, v.getDayOfMonth)),
    v => calendar => v.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)))

  def enumerationSpinnerFieldAccess[E <: Ordered[_]](enum: Enumeration): ViewFieldAccess[Spinner,E] = {
    val valueArray: Array[E] = enum.values.toArray.asInstanceOf[Array[E]]
//todo test that this clears the Spinner
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