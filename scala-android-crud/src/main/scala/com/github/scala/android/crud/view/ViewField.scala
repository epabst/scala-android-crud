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
import collection.immutable

/** A Map of ViewKey with values.
  * Wraps a map so that it is distinguished from persisted fields.
  */
case class ViewKeyMap(map: Map[PlatformTypes#ViewKey,Any])
        extends immutable.Map[PlatformTypes#ViewKey,Any] with immutable.MapLike[PlatformTypes#ViewKey,Any,ViewKeyMap] {
  override def empty = ViewKeyMap(Map.empty[PlatformTypes#ViewKey,Any])
  override def get(key: PlatformTypes#ViewKey) = map.get(key)
  override def iterator = map.iterator
  override def -(key: PlatformTypes#ViewKey) = ViewKeyMap(map - key)
  override def +[B1 >: Any](kv: (PlatformTypes#ViewKey, B1)) = ViewKeyMap(map + kv)
}

/**
 * PortableField for Views.
 * @param defaultLayout the default layout used as an example and by [[com.github.scala.android.crud.generate.CrudUIGenerator]].
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/16/11
 * Time: 6:30 AM
 */
class ViewField[T](val defaultLayout: FieldLayout, dataField: PortableField[T]) extends DelegatingPortableField[T] {
  protected def delegate = dataField
}

object ViewField extends PlatformTypes with Logging {
  /** PortableField for a View resource within a given parent View */
  protected abstract class BaseViewIdField[T](childViewField: PortableField[T])
          extends FieldWithDelegate[T] with TransformerUsingSetter[T] {
    protected def viewResourceIdOpt: Option[ViewKey]
    protected def viewResourceIdOrError: ViewKey

    lazy val viewKeyMapField =
      viewResourceIdOpt.map { key =>
        readOnly[ViewKeyMap,T](_.get(key).map(_.asInstanceOf[T])) +
                transformOnlyDirect[ViewKeyMap,T](map => value => map + (key -> value), _ - key)
      }.getOrElse(emptyField)

    object ChildView {
      def unapply(target: Any): Option[View] = target match {
        case view: View => viewResourceIdOpt.flatMap(id => Option(view.findViewById(id)))
        case activity: Activity => viewResourceIdOpt.flatMap(id => Option(activity.findViewById(id)))
        case _ => None
      }
    }

    protected def delegate = childViewField + viewKeyMapField

    def getter = viewKeyMapField.getter orElse {
      case ChildView(childView) if childViewField.getter.isDefinedAt(childView) =>
        childViewField.getter(childView)
    }

    def setter = viewKeyMapField.setter orElse {
      case ChildView(childView) if childViewField.setter.isDefinedAt(childView) =>
        childViewField.setter(childView)
    }
  }

  /** PortableField for a View resource within a given parent View */
  class ViewIdField[T](val viewResourceId: ViewKey, childViewField: PortableField[T])
          extends BaseViewIdField[T](childViewField) {
    protected def viewResourceIdOpt = Some(viewResourceId)
    protected def viewResourceIdOrError = viewResourceId
    override def toString = "viewId(" + viewResourceId + ", " + childViewField + ")"
  }

  /** PortableField for a View resource within a given parent View.
    * @param rIdClasses a list of R.id classes that may contain the id.
    */
  class ViewIdNameField[T](val viewResourceIdName: String, childViewField: PortableField[T], rIdClasses: Seq[Class[_]])
          extends BaseViewIdField[T](childViewField) {
    protected lazy val viewResourceIdOpt = findResourceIdWithName(rIdClasses, viewResourceIdName)
    protected def viewResourceIdOrError = resourceIdWithName(rIdClasses, viewResourceIdName)
    override def toString = "viewId(" + viewResourceIdName + ", " + childViewField + ")"
  }

  val textView: ViewField[String] = new ViewField[String](nameLayout,
    fieldDirect[TextView,String](v => toOption(v.getText.toString.trim), v => v.setText(_), _.setText(""))) {
    override def toString = "textView"
  }
  def textViewWithInputType(inputType: String) = new ViewField[String](textLayout(inputType), textView)
  lazy val phoneView: PortableField[String] = textViewWithInputType("phone")
  lazy val doubleView: PortableField[Double] = new ViewField[Double](doubleLayout, formatted(textView))
  lazy val currencyView = new ViewField[Double](currencyLayout, formatted(currencyValueFormat, textView))
  lazy val intView: PortableField[Int] = new ViewField[Int](intLayout, formatted[Int](textView))
  lazy val longView: PortableField[Long] = new ViewField[Long](longLayout, formatted[Long](textView))

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

  val calendarDateView: PortableField[Calendar] = new ViewField[Calendar](datePickerLayout,
    formatted(calendarValueFormat, textView) + fieldDirect[DatePicker,Calendar](
      v => Some(new GregorianCalendar(v.getYear, v.getMonth, v.getDayOfMonth)),
      v => calendar => v.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)))) {
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
    new ViewField[E](defaultLayout, adapterField + formatted[E](enumFormat(enum), textView)) {
      override def toString = "enumerationView(" + enum.getClass.getSimpleName + ")"
    }
  }

  def uriIdField(entityName: String): FieldGetter[UriPath,ID] = {
    PortableField.readOnly[UriPath,ID](_.findId(entityName))
  }
}