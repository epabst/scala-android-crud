package com.github.scala.android.crud.view

import xml.Elem

/** The layout piece for a field.
  * It provides the XML for the part of an Android Layout that corresponds to a single field.
  * Standards attributes are separately added such as android:id and those needed by the parent View.
  * @author Eric Pabst (epabst@gmail.com)
  */
abstract class FieldLayout { self =>
  def displayXml: Elem
  def editXml: Elem
  /** Returns a similar FieldLayout but where the editXml is overridden to use displayXml. */
  lazy val displayOnly: FieldLayout = new FieldLayout {
    def displayXml = self.displayXml
    def editXml = self.displayXml
  }
}

object FieldLayout {
  def textLayout(inputType: String) = new FieldLayout {
    def displayXml = <TextView/>
    def editXml = <EditText android:inputType={inputType}/>
  }

  lazy val nameLayout = textLayout("textCapWords")
  lazy val intLayout = textLayout("number|numberSigned")
  lazy val longLayout = textLayout("number|numberSigned")
  lazy val doubleLayout = textLayout("numberDecimal|numberSigned")
  lazy val currencyLayout = textLayout("numberDecimal|numberSigned")
  lazy val datePickerLayout = new FieldLayout {
    def displayXml = <TextView/>
    def editXml = <DatePicker/>
  }
  lazy val dateTextLayout = textLayout("date")

  private[crud] def toDisplayName(id: String): String = {
    var makeUpperCase = true
    val displayName = id.collect {
      case c if Character.isUpperCase(c) =>
        makeUpperCase = false
        " " + c
      case '_' =>
        makeUpperCase = true
        " "
      case c if makeUpperCase =>
        makeUpperCase = false
        Character.toUpperCase(c)
      case c => c.toString
    }.mkString
    displayName.stripPrefix(" ")
  }
}
