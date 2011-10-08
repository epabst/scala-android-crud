package com.github.scala.android.crud.view

import org.junit.runner.RunWith
import org.scalatest.matchers.MustMatchers
import com.github.triangle.PortableField._
import com.github.scala.android.crud.persistence.CursorField._
import ViewField._
import android.view.View
import com.xtremelabs.robolectric.RobolectricTestRunner
import org.junit.Test
import android.content.Context
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import com.github.scala.android.crud.action.UriPath
import java.util.Arrays
import android.widget._


/**
 * A behavior specification for {@link ViewField}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/9/11
 * Time: 7:59 PM
 */

@RunWith(classOf[RobolectricTestRunner])
class ViewFieldSpec extends MustMatchers with MockitoSugar {
  class MyEntity(var string: String, var number: Int)
  val context = mock[Context]
  val itemLayoutId = android.R.layout.simple_spinner_dropdown_item

  @Test
  def itMustBeEasilyInstantiableForAView() {
    class MyView(context: Context, var status: String) extends View(context)

    val stringField =
      persisted[String]("name") +
      viewId(101, textView) +
      viewId(102, fieldDirect[MyView,String](v => v.status, v => v.status = _))
    stringField must not be (null)
  }

  @Test
  def itMustClearTheViewIfEmpty() {
    val viewGroup = mock[View]
    val view1 = mock[TextView]
    val view2 = mock[TextView]
    val view3 = mock[TextView]
    stub(viewGroup.findViewById(101)).toReturn(view1)
    stub(viewGroup.findViewById(102)).toReturn(view2)
    stub(viewGroup.findViewById(103)).toReturn(view3)
    val stringField =
      viewId(101, textView) +
      viewId(102, fieldDirect[TextView,String](v => Option(v.getText.toString), v => v.setText(_), _.setText("Please Fill")))
    stringField.setValue(viewGroup, None)
    verify(view1).setText("")
    verify(view2).setText("Please Fill")

    val intField = viewId(103, intView)
    intField.setValue(viewGroup, None)
    verify(view3).setText("")
  }

  @Test
  def itMustOnlyCopyToAndFromViewByIdIfTheRightType() {
    val group = mock[View]
    val view = mock[TextView]
    stub(group.findViewById(56)).toReturn(view)
    val stringField = fieldDirect[MyEntity,String](e => e.string, e => e.string = _) +
      viewId(56, fieldDirect[Spinner,String](
        _ => throw new IllegalStateException("must not be called"),
        _ => throw new IllegalStateException("must not be called")))
    val myEntity1 = new MyEntity("my1", 1)
    stringField.copy(myEntity1, group) //does nothing
    stringField.copy(group, myEntity1) //does nothing
  }

  @Test
  def itMustOnlyCopyToAndFromViewByIdIfIdIsFound() {
    val stringField = fieldDirect[MyEntity,String](e => e.string, e => e.string = _) +
      viewId(56, fieldDirect[Spinner,String](
        _ => throw new IllegalStateException("must not be called"),
        _ => throw new IllegalStateException("must not be called")))
    val myEntity1 = new MyEntity("my1", 1)
    val group = new LinearLayout(context)
    val view = new Spinner(context)
    view.setId(100)
    group.addView(view)
    stringField.copy(myEntity1, group) //does nothing
    stringField.copy(group, myEntity1) //does nothing
  }

  @Test
  def itMustHandleUnparseableValues() {
    val intField = intView + fieldDirect[MyEntity,Int](e => e.number, e => e.number = _)
    val view = new TextView(context)
    view.setText("twenty")
    intField.getter(view) must be (None)

    val entity = new MyEntity("my1", 30)
    intField.copy(view, entity)
    entity.number must be (30)
  }

  @Test
  def itMustGetTheIdForAnEntityNameFromTheUriPath() {
    val field = uriIdField("foo")
    val uripath = UriPath("hello", "1", "foo", "4", "bar", "3")
    field.getter(uripath) must be (Some(4))

    val uripath2 = UriPath("hello", "1", "foo")
    field.getter(uripath2) must be (None)

    val uripath3 = UriPath("hello")
    field.getter(uripath3) must be (None)

    val uripath4 = UriPath()
    field.getter(uripath4) must be (None)

    val uripath5 = UriPath("4")
    field.getter(uripath5) must be (None)
  }

  @Test
  def itMustConvertNullToNone() {
    val field = textView
    val view = new TextView(context)
    view.setText(null)
    field.getter(view) must be (None)

    view.setText("")
    field.getter(view) must be (None)
  }

  @Test
  def itMustTrimStrings() {
    val field = textView
    val view = new TextView(context)
    view.setText("  ")
    field.getter(view) must be (None)

    view.setText(" hello world ")
    field.getter(view) must be (Some("hello world"))
  }

  @Test
  def adapterViewFieldMustSetTheAdapterForAnAdapterView() {
    val list = Arrays.asList("a", "b", "c")
    val field = ViewField.adapterViewField[String,BaseAdapter](view => new ArrayAdapter[String](context, itemLayoutId, list), list.indexOf(_))
    val adapterView = new Spinner(context)
    field.setValue(adapterView, Some("c"))
    val adapter = adapterView.getAdapter
    (0 to (adapter.getCount - 1)).toList.map(adapter.getItem(_)) must be (List("a", "b", "c"))
  }

  @Test
  def adapterViewFieldMustSetThePositionCorrectly() {
    val list = Arrays.asList("a", "b", "c")
    val field = ViewField.adapterViewField[String,BaseAdapter](view => new ArrayAdapter[String](context, itemLayoutId, list), list.indexOf(_))
    val adapterView = new Spinner(context)
    field.setValue(adapterView, Some("c"))
    adapterView.getSelectedItemPosition must be (2)
  }

  @Test
  def adapterViewFieldMustHandleInvalidValueForAnAdapterView() {
    val list = Arrays.asList("a", "b", "c")
    val field = ViewField.adapterViewField[String,BaseAdapter](view => new ArrayAdapter[String](context, itemLayoutId, list), list.indexOf(_))
    val adapterView = new Spinner(context)
    field.setValue(adapterView, Some("blah"))
    adapterView.getSelectedItemPosition must be (AdapterView.INVALID_POSITION)
  }
}
