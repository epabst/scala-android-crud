package com.github.scala.android.crud.view

import _root_.android.app.Activity
import _root_.android.view.View
import _root_.android.widget.{ArrayAdapter, Spinner, DatePicker, TextView}
import com.github.scala.android.crud.common.PlatformTypes
import com.github.triangle._
import PortableField._
import java.util.{Calendar, Date, GregorianCalendar}
import com.github.scala.android.crud.action.UriPath
import FieldLayout._
import ValueFormat._
import AndroidResourceAnalyzer._
import com.github.triangle.Converter._

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

  def enumerationView[E <: Enumeration#Value](enum: Enumeration): PortableField[E] = {
    val valueArray: Array[E] = enum.values.toArray.asInstanceOf[Array[E]]
    debug("enumerationView values: " + valueArray.mkString(","))
    val defaultLayout = new FieldLayout {
      def displayXml = <TextView/>
      def editXml = <Spinner android:drawSelectorOnTop = "true"/>
    }
    new ViewField[E](defaultLayout) {
      private val spinnerField: PortableField[E] = fieldDirect[Spinner,E](v => Option(v.getSelectedItem.asInstanceOf[E]), spinner => value => {
        //don't do it again if already done from a previous time
        debug("Spinner.getAdapter is " + spinner.getAdapter)
        debug("Spinner.getAdapter.getCount is " + spinner.getAdapter.getCount)
        debug("Spinner.getAdapter.items are " + (0 to spinner.getAdapter.getCount).map(spinner.getAdapter.getItem(_)).mkString(","))
        if (spinner.getAdapter == null) {
          debug("Setting values in Spinner to be " + valueArray.mkString(","))
          spinner.setAdapter(new ArrayAdapter[E](spinner.getContext, _root_.android.R.layout.simple_spinner_dropdown_item, valueArray))
        }
        spinner.setSelection(valueArray.indexOf(value))
      })
      protected def delegate = spinnerField + formatted[E](enumFormat(enum), textView)

      override def toString = "enumerationView(" + enum.getClass.getSimpleName + ")"
    }
  }

  def uriIdField(entityName: String): FieldGetter[UriPath,ID] = {
    PortableField.readOnly[UriPath,ID](_.findId(entityName))
  }
}