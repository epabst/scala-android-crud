package com.github.scala.android.crud.view

import _root_.android.app.Activity
import _root_.android.view.View
import com.github.scala.android.crud.common.PlatformTypes
import com.github.triangle._
import PortableField._
import java.util.{Calendar, Date, GregorianCalendar}
import com.github.scala.android.crud.action.UriPath
import FieldLayout._
import ValueFormat._
import AndroidResourceAnalyzer._
import com.github.triangle.Converter._
import android.widget._

/**
 * PortableField for Views.
 * @param defaultLayout the default layout used as an example and by [[com.github.scala.android.crud.generate.CrudUIGenerator]].
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/16/11
 * Time: 6:30 AM
 */
abstract class ViewField[T](val defaultLayout: FieldLayout) extends DelegatingPortableField[T]

object ViewField extends PlatformTypes with Logging {
  private class ChildViewById(viewResourceId: ViewKey) {
    def unapply(target: Any): Option[View] = target match {
      case view: View => Option(view.findViewById(viewResourceId))
      case activity: Activity => Option(activity.findViewById(viewResourceId))
      case _ => None
    }
  }

  /**
   * @param rIdClasses a list of R.id classes that may contain the id.
   */
  private class ChildViewByIdName(viewResourceIdName: String, rIdClasses: Seq[Class[_]]) {
    val childViewById = findResourceIdWithName(rIdClasses, viewResourceIdName).map(new ChildViewById(_))

    def unapply(target: Any): Option[View] = childViewById.flatMap(_.unapply(target))
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

  /** PortableField for a View resource within a given parent View */
  class ViewIdNameField[T](val viewResourceIdName: String, childViewField: PortableField[T], rIdClasses: Seq[Class[_]])
          extends FieldWithDelegate[T] with TransformerUsingSetter[T] {
    private object ChildView extends ChildViewByIdName(viewResourceIdName, rIdClasses)

    protected def delegate = childViewField

    def getter = {
      case ChildView(childView) if childViewField.getter.isDefinedAt(childView) =>
        childViewField.getter(childView)
    }

    def setter = {
      case ChildView(childView) if childViewField.setter.isDefinedAt(childView) =>
        childViewField.setter(childView)
    }

    override def toString = "viewId(" + viewResourceIdName + ", " + childViewField + ")"
  }

  val textView: ViewField[String] = new ViewField[String](nameLayout) {
    protected def delegate = fieldDirect[TextView,String](v => toOption(v.getText.toString.trim), v => v.setText(_), _.setText(""))

    override def toString = "textView"
  }
  def textViewWithInputType(inputType: String): PortableField[String] = new ViewField[String](textLayout(inputType)) {
    protected def delegate = textView
  }
  lazy val phoneView: PortableField[String] = textViewWithInputType("phone")
  lazy val doubleView: PortableField[Double] = new ViewField[Double](doubleLayout) {
    protected def delegate = formatted(textView)
  }
  lazy val currencyView: PortableField[Double] = new ViewField[Double](currencyLayout) {
    protected def delegate = formatted(currencyValueFormat, textView)
  }
  lazy val intView: PortableField[Int] = new ViewField[Int](intLayout) {
    protected def delegate = formatted[Int](textView)
  }

  lazy val longView: PortableField[Long] = new ViewField[Long](longLayout) {
    protected def delegate = formatted[Long](textView)
  }

  def viewId[T](viewResourceId: ViewKey, childViewField: PortableField[T]): ViewIdField[T] = {
    new ViewIdField[T](viewResourceId, childViewField)
  }

  /**
   * This should be used when R.id doesn't yet have the needed name, and used like this:
   * {{{viewId(classOf[R.id], "name", ...)}}}
   * Which is conceptually identical to
   * {{{viewId(R.id.name, ...)}}}.
   */
  def viewId[T](rIdClass: Class[_], viewResourceIdName: String, childViewField: PortableField[T]): PortableField[T] =
    new ViewIdNameField[T](viewResourceIdName, childViewField, detectRIdClasses(rIdClass))

  private def toOption(string: String): Option[String] = if (string == "") None else Some(string)

  val calendarDateView: PortableField[Calendar] = new ViewField[Calendar](datePickerLayout) {
    protected def delegate = formatted(calendarValueFormat, textView) + fieldDirect[DatePicker,Calendar](
      v => Some(new GregorianCalendar(v.getYear, v.getMonth, v.getDayOfMonth)),
      v => calendar => v.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)))

    override def toString = "calendarDateView"
  }

  implicit val dateView: PortableField[Date] = converted(dateToCalendar, calendarToDate, calendarDateView)

  /**
    * @param adapterFactory a function that takes the adapter View and returns the Adapter to put into it.
    * @param positionFinder a function that takes a value and returns its position in the Adapter
    */
  private[view] def adapterViewField[T,A <: Adapter](adapterFactory: AdapterView[A] => A, positionFinder: T => Int): PortableField[T] = {
    field[AdapterView[A], T](v => Option(v.getSelectedItem.asInstanceOf[T]), adapterView => valueOpt => {
      //don't do it again if already done from a previous time
      if (adapterView.getAdapter == null) {
        val adapter: A = adapterFactory(adapterView)
        adapterView.setAdapter(adapter)
      }
      adapterView.setSelection(valueOpt.map(positionFinder(_)).getOrElse(-1))
    })
  }

  def enumerationView[E <: Enumeration#Value](enum: Enumeration): PortableField[E] = {
    val itemViewResourceId = _root_.android.R.layout.simple_spinner_dropdown_item
    val defaultLayout = new FieldLayout {
      def displayXml = <TextView/>
      def editXml = <Spinner android:drawSelectorOnTop = "true"/>
    }
    val valueArray: Array[E] = enum.values.toArray.asInstanceOf[Array[E]]
    val adapterField = adapterViewField[E, BaseAdapter](
      view => new ArrayAdapter[E](view.getContext, itemViewResourceId, valueArray),
      value => valueArray.indexOf(value))
    new ViewField[E](defaultLayout) {
      protected def delegate = adapterField + formatted[E](enumFormat(enum), textView)

      override def toString = "enumerationView(" + enum.getClass.getSimpleName + ")"
    }
  }

  def uriIdField(entityName: String): FieldGetter[UriPath,ID] = {
    PortableField.readOnly[UriPath,ID](_.findId(entityName))
  }
}