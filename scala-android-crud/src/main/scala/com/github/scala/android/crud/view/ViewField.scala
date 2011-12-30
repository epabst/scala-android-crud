package com.github.scala.android.crud.view

import com.github.scala.android.crud.common.PlatformTypes._
import com.github.triangle._
import PortableField._
import java.util.{Calendar, Date, GregorianCalendar}
import FieldLayout._
import ValueFormat._
import com.github.triangle.Converter._
import android.widget._
import scala.collection.JavaConversions._
import com.github.scala.android.crud.view.AndroidResourceAnalyzer._
import android.net.Uri

/** A Map of ViewKey with values.
  * Wraps a map so that it is distinguished from persisted fields.
  */
case class ViewKeyMap(map: Map[ViewKey,Any]) {
  def get(key: ViewKey) = map.get(key)
  def iterator = map.iterator
  def -(key: ViewKey) = ViewKeyMap(map - key)
  def +[B1 >: Any](kv: (ViewKey, B1)) = ViewKeyMap(map + kv)
}

object ViewKeyMap {
  def empty = ViewKeyMap()
  def apply(elems: (ViewKey,Any)*): ViewKeyMap = new ViewKeyMap(Map(elems: _*))
}

/** PortableField for Views.
  * @param defaultLayout the default layout used as an example and by [[com.github.scala.android.crud.generate.CrudUIGenerator]].
  * @author Eric Pabst (epabst@gmail.com)
  */
class ViewField[T](val defaultLayout: FieldLayout, dataField: PortableField[T]) extends DelegatingPortableField[T] {
  protected def delegate = dataField

  lazy val displayOnly: ViewField[T] = new ViewField[T](defaultLayout.displayOnly, dataField)
  def withDefaultLayout(newDefaultLayout: FieldLayout): ViewField[T] = new ViewField[T](newDefaultLayout, dataField)
}

object ViewField {
  def viewId[T](viewResourceId: ViewKey, childViewField: PortableField[T]): PortableField[T] =
    new ViewIdField[T](viewResourceId, childViewField).withViewKeyMapField

  /** This should be used when R.id doesn't yet have the needed name, and used like this:
    * {{{viewId(classOf[R.id], "name", ...)}}}
    * Which is conceptually identical to
    * {{{viewId(R.id.name, ...)}}}.
    */
  def viewId[T](rIdClass: Class[_], viewResourceIdName: String, childViewField: PortableField[T]): PortableField[T] =
    new ViewIdNameField[T](viewResourceIdName, childViewField, detectRIdClasses(rIdClass)).withViewKeyMapField

  val textView: ViewField[String] = new ViewField[String](nameLayout,
    Getter[TextView,String](v => toOption(v.getText.toString.trim)).withSetter(v => v.setText(_), _.setText(""))) {
    override def toString = "textView"
  }
  def textViewWithInputType(inputType: String): ViewField[String] = new ViewField[String](textLayout(inputType), textView)
  lazy val phoneView: ViewField[String] = textViewWithInputType("phone")
  lazy val doubleView: ViewField[Double] = new ViewField[Double](doubleLayout, formatted(textView))
  lazy val currencyView = new ViewField[Double](currencyLayout, formatted(currencyValueFormat, textView))
  lazy val intView: ViewField[Int] = new ViewField[Int](intLayout, formatted[Int](textView))
  lazy val longView: ViewField[Long] = new ViewField[Long](longLayout, formatted[Long](textView))
  /** Specifically an EditText view in order to get different behavior compared to a plain TextView. */
  val editTextView: ViewField[String] = new ViewField[String](nameLayout,
    Getter[EditText,String](v => toOption(v.getText.toString.trim)).withSetter(v => v.setText(_), _.setText(""))) {
    override def toString = "editTextView"
  }

  private def toOption(string: String): Option[String] = if (string == "") None else Some(string)

  val calendarDateView: ViewField[Calendar] = new ViewField[Calendar](datePickerLayout,
    Setter[Calendar] {
      case view: EditText => _.foreach(value => view.setText(calendarValueFormat.toString(value)))
      case view: TextView => _.foreach(value => view.setText(calendarDisplayValueFormat.toString(value)))
      case picker: DatePicker => valueOpt =>
        val calendar = valueOpt.getOrElse(Calendar.getInstance())
        picker.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
    } + Getter(formatted(calendarValueFormat, textView).getter) +
            Getter((p: DatePicker) => Some(new GregorianCalendar(p.getYear, p.getMonth, p.getDayOfMonth)))) {
    override def toString = "calendarDateView"
  }

  implicit val dateView: ViewField[Date] =
    new ViewField[Date](calendarDateView.defaultLayout, converted(dateToCalendar, calendarToDate, calendarDateView)) {
      override def toString = "dateView"
    }

  /** @param adapterFactory a function that takes the adapter View and returns the Adapter to put into it.
    * @param positionFinder a function that takes a value and returns its position in the Adapter
    */
  private[view] def adapterViewField[T,A <: Adapter](adapterFactory: AdapterView[A] => A, positionFinder: T => Int): PortableField[T] = {
    Getter[AdapterView[A], T](v => Option(v.getSelectedItem.asInstanceOf[T])).withSetter(adapterView => valueOpt => {
      //don't do it again if already done from a previous time
      if (adapterView.getAdapter == null) {
        val adapter: A = adapterFactory(adapterView)
        adapterView.setAdapter(adapter)
      }
      adapterView.setSelection(valueOpt.map(positionFinder(_)).getOrElse(-1))
    })
  }

  def enumerationView[E <: Enumeration#Value](enum: Enumeration): ViewField[E] = {
    val itemViewResourceId = _root_.android.R.layout.simple_spinner_dropdown_item
    val defaultLayout = new FieldLayout {
      def displayXml = <TextView/>
      def editXml = <Spinner android:drawSelectorOnTop = "true"/>
    }
    val valueArray: List[E] = enum.values.toList.asInstanceOf[List[E]]
    val adapterField = adapterViewField[E, BaseAdapter](
      view => new ArrayAdapter[E](view.getContext, itemViewResourceId, valueArray),
      value => valueArray.indexOf(value))
    new ViewField[E](defaultLayout, adapterField + formatted[E](enumFormat(enum), textView)) {
      override def toString = "enumerationView(" + enum.getClass.getSimpleName + ")"
    }
  }

  lazy val capturedImageView: ViewField[Uri] = CapturedImageView
}
