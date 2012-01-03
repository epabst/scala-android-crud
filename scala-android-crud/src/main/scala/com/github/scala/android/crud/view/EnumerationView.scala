package com.github.scala.android.crud.view

import com.github.triangle.PortableField._
import com.github.triangle.ValueFormat._
import android.widget.{AdapterView, Adapter, ArrayAdapter, BaseAdapter}
import com.github.triangle.{Getter, PortableField}
import scala.collection.JavaConversions._

/** A ViewField for an [[scala.Enumeration]].
  * @author Eric Pabst (epabst@gmail.com)
  */
case class EnumerationView[E <: Enumeration#Value](enum: Enumeration)
  extends ViewField[E](FieldLayout(displayXml = <TextView/>, editXml = <Spinner android:drawSelectorOnTop = "true"/>)) {

  private val itemViewResourceId = _root_.android.R.layout.simple_spinner_dropdown_item
  private val valueArray: List[E] = enum.values.toList.asInstanceOf[List[E]]

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

  private val adapterField = adapterViewField[E, BaseAdapter](
    view => new ArrayAdapter[E](view.getContext, itemViewResourceId, valueArray),
    value => valueArray.indexOf(value))
  protected val delegate = adapterField + formatted[E](enumFormat(enum), ViewField.textView)
  override def toString = "EnumerationView(" + enum.getClass.getSimpleName + ")"
}
