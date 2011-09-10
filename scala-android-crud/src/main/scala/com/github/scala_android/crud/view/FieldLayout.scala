package com.github.scala_android.crud.view

import xml.Elem

/**
 * The layout piece for a field.
 * It provides the XML for the part of an Android Layout that corresponds to a single field.
 * Standards attributes are separately added such as android:id and those needed by the parent View.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 9/10/11
 * Time: 12:08 AM
 */
abstract class FieldLayout {
  def displayXml: Elem
  def editXml: Elem
}

object FieldLayout {
  def textLayout(inputType: String) = new FieldLayout {
    def displayXml = <TextView/>
    def editXml = <EditText android:inputType={inputType}/>
  }

  lazy val nameLayout = textLayout("textCapWords")
  lazy val intLayout = textLayout("number|numberSigned")
  lazy val doubleLayout = textLayout("numberDecimal|numberSigned")
  lazy val currencyLayout = textLayout("numberDecimal|numberSigned")
  lazy val datePickerLayout = new FieldLayout {
    def displayXml = <TextView/>
    def editXml = <DatePicker/>
  }
  lazy val dateTextLayout = textLayout("date")

  private[crud] def toId(displayName: String): String = {
    val id = displayName.collect {
      case c if Character.isJavaIdentifierPart(c) => c
    }.dropWhile(!Character.isJavaIdentifierStart(_))
    id.head.toLower + id.tail
  }
}
