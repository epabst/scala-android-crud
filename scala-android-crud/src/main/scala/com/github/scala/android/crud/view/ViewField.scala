package com.github.scala.android.crud.view

import com.github.scala.android.crud.common.PlatformTypes._
import com.github.triangle._
import PortableField._
import java.util.{Calendar, Date, GregorianCalendar}
import FieldLayout._
import ValueFormat._
import com.github.triangle.Converter._
import android.widget._
import com.github.scala.android.crud.view.AndroidResourceAnalyzer._
import android.view.View

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

/** An extractor to get the View from the items being copied from. */
object ViewExtractor extends Field(identityField[View])

/** PortableField for Views.
  * @param defaultLayout the default layout used as an example and by [[com.github.scala.android.crud.generate.CrudUIGenerator]].
  * @author Eric Pabst (epabst@gmail.com)
  */
abstract class ViewField[T](val defaultLayout: FieldLayout) extends DelegatingPortableField[T] { self =>
  lazy val displayOnly: ViewField[T] = ViewField[T](defaultLayout.displayOnly, this)

  def withDefaultLayout(newDefaultLayout: FieldLayout): ViewField[T] = ViewField[T](newDefaultLayout, this)
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

  def apply[T](defaultLayout: FieldLayout, dataField: PortableField[T]): ViewField[T] = new ViewField[T](defaultLayout) {
    protected def delegate = dataField
  }

  val textView: ViewField[String] = new ViewField[String](nameLayout) {
    val delegate = Getter[TextView,String](v => toOption(v.getText.toString.trim)).withSetter(v => v.setText(_), _.setText(""))
    override def toString = "textView"
  }
  def textViewWithInputType(inputType: String): ViewField[String] = textView.withDefaultLayout(textLayout(inputType))
  lazy val phoneView: ViewField[String] = textViewWithInputType("phone")
  lazy val doubleView: ViewField[Double] = ViewField[Double](doubleLayout, formatted(textView))
  lazy val currencyView = ViewField[Double](currencyLayout, formatted(currencyValueFormat, textView))
  lazy val intView: ViewField[Int] = ViewField[Int](intLayout, formatted[Int](textView))
  lazy val longView: ViewField[Long] = ViewField[Long](longLayout, formatted[Long](textView))
  /** Specifically an EditText view in order to get different behavior compared to a plain TextView. */
  val editTextView: ViewField[String] = new ViewField[String](nameLayout) {
    val delegate = Getter[EditText,String](v => toOption(v.getText.toString.trim)).withSetter(v => v.setText(_), _.setText(""))
    override def toString = "editTextView"
  }

  private def toOption(string: String): Option[String] = if (string == "") None else Some(string)

  val calendarDateView: ViewField[Calendar] = new ViewField[Calendar](datePickerLayout) {
    val delegate = Setter[Calendar] {
      case view: EditText => _.foreach(value => view.setText(calendarValueFormat.toString(value)))
      case view: TextView => _.foreach(value => view.setText(calendarDisplayValueFormat.toString(value)))
      case picker: DatePicker => valueOpt =>
        val calendar = valueOpt.getOrElse(Calendar.getInstance())
        picker.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
    } + Getter(formatted(calendarValueFormat, textView).getter) +
            Getter((p: DatePicker) => Some(new GregorianCalendar(p.getYear, p.getMonth, p.getDayOfMonth)))
    override def toString = "calendarDateView"
  }

  implicit val dateView: ViewField[Date] =
    new ViewField[Date](calendarDateView.defaultLayout) {
      val delegate = converted(dateToCalendar, calendarToDate, calendarDateView)
      override def toString = "dateView"
    }

  @Deprecated //use EnumerationView
  def enumerationView[E <: Enumeration#Value](enum: Enumeration): ViewField[E] = EnumerationView[E](enum)
}
