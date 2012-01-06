package com.github.scala.android.crud.view

import org.junit.runner.RunWith
import org.scalatest.matchers.MustMatchers
import com.github.triangle.PortableField._
import com.xtremelabs.robolectric.RobolectricTestRunner
import org.junit.Test
import org.scalatest.mock.MockitoSugar
import android.widget._
import java.util.Locale
import android.content.Context

/** A behavior specification for [[com.github.scala.android.crud.view.ViewField]].
  * @author Eric Pabst (epabst@gmail.com)
  */

@RunWith(classOf[RobolectricTestRunner])
class EnumerationViewSpec extends MustMatchers with MockitoSugar {
  class MyEntity(var string: String, var number: Int)
  val context = mock[Context]
  val itemLayoutId = android.R.layout.simple_spinner_dropdown_item
  Locale.setDefault(Locale.US)
  object MyEnum extends Enumeration {
    val A = Value("a")
    val B = Value("b")
    val C = Value("c")
  }
  val enumerationView = EnumerationView[MyEnum.Value](MyEnum)

  @Test
  def itMustSetTheAdapterForAnAdapterView() {
    val adapterView = new Spinner(context)
    enumerationView.setValue(adapterView, Some(MyEnum.C))
    val adapter = adapterView.getAdapter
    (0 to (adapter.getCount - 1)).toList.map(adapter.getItem(_)) must be (List(MyEnum.A, MyEnum.B, MyEnum.C))
  }

  @Test
  def itMustSetTheAdapterForAnAdapterViewEvenIfTheValueIsNotSet() {
    val adapterView = new Spinner(context)
    enumerationView.setValue(adapterView, None)
    val adapter = adapterView.getAdapter
    (0 to (adapter.getCount - 1)).toList.map(adapter.getItem(_)) must be (List(MyEnum.A, MyEnum.B, MyEnum.C))
  }

  @Test
  def itMustSetThePositionCorrectly() {
    val adapterView = new Spinner(context)
    enumerationView.setValue(adapterView, MyEnum.C)
    adapterView.getSelectedItemPosition must be (2)
  }

  @Test
  def itMustHandleInvalidValueForAnAdapterView() {
    val field = enumerationView
    val adapterView = new Spinner(context)
    field.setValue(adapterView, None)
    adapterView.getSelectedItemPosition must be (AdapterView.INVALID_POSITION)
  }
}
