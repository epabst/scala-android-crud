package com.github.scala.android.crud.view

import com.github.triangle.PortableField._
import com.github.triangle.ValueFormat._
import android.widget.{AdapterView, ArrayAdapter, BaseAdapter}
import com.github.triangle.Getter
import scala.collection.JavaConversions._

/** A ViewField for an [[scala.Enumeration]].
  * @author Eric Pabst (epabst@gmail.com)
  */
case class EnumerationView[E <: Enumeration#Value](enum: Enumeration)
  extends ViewField[E](FieldLayout(displayXml = <TextView/>, editXml = <Spinner android:drawSelectorOnTop = "true"/>)) {

  private val itemViewResourceId = _root_.android.R.layout.simple_spinner_dropdown_item
  private val valueArray: List[E] = enum.values.toList.asInstanceOf[List[E]]

  protected val delegate = Getter[AdapterView[BaseAdapter],E](v => Option(v.getSelectedItem.asInstanceOf[E])).
    withSetter { adapterView => valueOpt =>
      //don't do it again if already done from a previous time
      if (adapterView.getAdapter == null) {
        val adapter = new ArrayAdapter[E](adapterView.getContext, itemViewResourceId, valueArray)
        adapterView.setAdapter(adapter)
      }
      adapterView.setSelection(valueOpt.map(valueArray.indexOf(_)).getOrElse(AdapterView.INVALID_POSITION))
    } + formatted[E](enumFormat(enum), ViewField.textView)

  override def toString = "EnumerationView(" + enum.getClass.getSimpleName + ")"
}
