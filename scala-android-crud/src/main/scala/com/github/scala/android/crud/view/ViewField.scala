package com.github.scala.android.crud.view

import _root_.android.app.Activity
import _root_.android.view.View
import com.github.scala.android.crud.common.PlatformTypes._
import com.github.triangle._
import PortableField._
import java.util.{Calendar, Date, GregorianCalendar}
import FieldLayout._
import ValueFormat._
import AndroidResourceAnalyzer._
import com.github.triangle.Converter._
import android.widget._
import scala.collection.JavaConversions._
import com.github.scala.android.crud.common.UriPath
import com.github.scala.android.crud.res.R
import com.github.scala.android.crud.action.Operation.toUri

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

/**
 * PortableField for Views.
 * @param defaultLayout the default layout used as an example and by [[com.github.scala.android.crud.generate.CrudUIGenerator]].
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/16/11
 * Time: 6:30 AM
 */
class ViewField[T](val defaultLayout: FieldLayout, dataField: PortableField[T]) extends DelegatingPortableField[T] {
  protected def delegate = dataField

  lazy val displayOnly: ViewField[T] = new ViewField[T](defaultLayout.displayOnly, dataField)
  def withDefaultLayout(newDefaultLayout: FieldLayout): ViewField[T] = new ViewField[T](newDefaultLayout, dataField)
}

object ViewField {
  /** PortableField for a View resource within a given parent View */
  protected abstract class BaseViewIdField[T](childViewField: PortableField[T])
          extends FieldWithDelegate[T] {
    protected def viewResourceIdOpt: Option[ViewKey]
    protected def viewResourceIdOrError: ViewKey

    lazy val viewKeyMapField: PortableField[T] =
      viewResourceIdOpt.map { key =>
        Getter[ViewKeyMap,T](_.get(key).map(_.asInstanceOf[T])).withTransformer(map => value => map + (key -> value), _ - key)
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

    def transformer[S <: AnyRef]: PartialFunction[S,Option[T] => S] = viewKeyMapField.transformer[S].orElse[S,Option[T] => S] {
      case ChildView(childView) if childViewField.transformer.isDefinedAt(childView) =>
        childViewField.transformer[View](childView).andThen[S](_.asInstanceOf[S])
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

  /**
    * @param adapterFactory a function that takes the adapter View and returns the Adapter to put into it.
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

  val capturedImageView: ViewField[UriPath] = {
    def setImageUri(imageView: ImageView, uriOpt: Option[UriPath]) {
      uriOpt match {
        case Some(uri) =>
          imageView.setTag(uri)
          imageView.setImageURI(toUri(uri))
        case None =>
          imageView.setImageResource(R.drawable.android_camera_256)
      }
    }

    def imageUri(imageView: ImageView): Option[UriPath] = Option(imageView.getTag.asInstanceOf[UriPath])

    val defaultLayout = new FieldLayout {
      def displayXml = <ImageView android:adjustViewBounds="true"/>

      def editXml = <ImageView android:adjustViewBounds="true"/>
    }
    new ViewField[UriPath](defaultLayout, Getter((v: ImageView) => imageUri(v)).withSetter(v => uri => setImageUri(v, uri)))
  }
}
