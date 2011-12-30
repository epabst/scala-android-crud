package com.github.scala.android.crud.view

import org.junit.runner.RunWith
import org.scalatest.matchers.MustMatchers
import com.github.triangle.PortableField._
import com.github.scala.android.crud.persistence.CursorField._
import ViewField._
import android.view.View
import com.xtremelabs.robolectric.RobolectricTestRunner
import org.junit.Test
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import com.github.scala.android.crud.common.UriPath
import UriPath.uriIdField
import android.widget._
import java.util.{Locale, GregorianCalendar, Calendar, Arrays}
import com.github.triangle.Getter
import com.github.scala.android.crud.action.OperationResponse
import android.net.Uri
import android.content.{Intent, Context}

/** A behavior specification for [[com.github.scala.android.crud.view.ViewField]].
  * @author Eric Pabst (epabst@gmail.com)
  */

@RunWith(classOf[RobolectricTestRunner])
class ViewFieldSpec extends MustMatchers with MockitoSugar {
  class MyEntity(var string: String, var number: Int)
  val context = mock[Context]
  val itemLayoutId = android.R.layout.simple_spinner_dropdown_item
  Locale.setDefault(Locale.US)

  @Test
  def itMustBeEasilyInstantiableForAView() {
    class MyView(context: Context, var status: String) extends View(context)

    val stringField =
      persisted[String]("name") +
      viewId(101, textView) +
      viewId(102, Getter[MyView,String](v => v.status).withSetter(v => v.status = _, noSetterForEmpty))
    stringField must not be (null)
  }

  @Test
  def itMustPopulateAViewKeyMap() {
    val stringField = persisted[String]("name") + viewId(101, textView)
    val map = stringField.transform(ViewKeyMap(), Map("name" -> "George"))
    map must be (ViewKeyMap(101 -> "George"))
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
      viewId(102, Getter[TextView,String](v => Option(v.getText.toString)).withSetter(v => v.setText(_), _.setText("Please Fill")))
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
    val stringField = Getter[MyEntity,String](e => e.string).withSetter(e => e.string = _, noSetterForEmpty) +
      viewId(56, Getter[Spinner,String](_ => throw new IllegalStateException("must not be called")).
        withSetter(_ => throw new IllegalStateException("must not be called")))
    val myEntity1 = new MyEntity("my1", 1)
    stringField.copy(myEntity1, group) //does nothing
    stringField.copy(group, myEntity1) //does nothing
  }

  @Test
  def itMustOnlyCopyToAndFromViewByIdIfIdIsFound() {
    val stringField = Getter[MyEntity,String](e => e.string).withSetter(e => e.string = _, noSetterForEmpty) +
      viewId(56, Getter[Spinner,String](_ => throw new IllegalStateException("must not be called")).
        withSetter(_ => throw new IllegalStateException("must not be called")))
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
    val intField = intView + Getter[MyEntity,Int](e => e.number).withSetter(e => e.number = _, noSetterForEmpty)
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
  def itMustFormatDatesForEditInShortFormat() {
    val field = calendarDateView
    val view = new EditText(context)
    field.setValue(view, Some(new GregorianCalendar(2020, Calendar.JANUARY, 20)))
    view.getText.toString must not(include ("Jan"))
    view.getText.toString must include ("1")
  }

  @Test
  def itMustFormatDatesForDisplayInDefaultFormat() {
    val field = calendarDateView
    val view = new TextView(context)
    field.setValue(view, Some(new GregorianCalendar(2020, Calendar.JANUARY, 20)))
    view.getText.toString must include ("Jan")
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
  def adapterViewFieldMustSetTheAdapterForAnAdapterViewEvenIfTheValueIsNotSet() {
    val list = Arrays.asList("a", "b", "c")
    val field = ViewField.adapterViewField[String,BaseAdapter](view => new ArrayAdapter[String](context, itemLayoutId, list), list.indexOf(_))
    val adapterView = new Spinner(context)
    field.setValue(adapterView, None)
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

  @Test
  def defaultLayoutMustBeOverriddenByDisplayLayout() {
    val viewField = textView
    viewField.displayOnly.defaultLayout.editXml must be (viewField.defaultLayout.displayXml)
  }

  @Test
  def defaultLayoutMustBeAbleToBeOverridden() {
    val viewField = textView
    val newLayout = FieldLayout.textLayout("numberDecimal")
    val adjustedViewField = viewField.withDefaultLayout(newLayout)
    adjustedViewField.defaultLayout.editXml must be (newLayout.editXml)
  }

  @Test
  def capturedImageViewMustGetImageUriFromOperationResponse() {
    val uri = Uri.parse("file://foo/bar.jpg")
    val TheViewId = 101
    val field = viewId(TheViewId, ViewField.capturedImageView)
    val outerView = mock[View]
    val view = mock[View]
    val intent = mock[Intent]
    stub(outerView.getId).toReturn(TheViewId)
    stub(outerView.findViewById(TheViewId)).toReturn(view)
    stub(intent.getData).toReturn(uri)
    stub(view.getTag(ViewField.DefaultValueTagKey)).toReturn(uri.toString)
    field.getterFromItem(List(OperationResponse(TheViewId, intent), outerView)) must be (Some(uri))
  }

  @Test
  def capturedImageViewMustGetImageUriFromViewTagOperationResponseDoesNotHaveIt() {
    val TheViewId = 101
    val field = viewId(TheViewId, ViewField.capturedImageView)
    val outerView = mock[View]
    val view = mock[View]
    stub(outerView.getId).toReturn(TheViewId)
    stub(outerView.findViewById(TheViewId)).toReturn(view)
    stub(view.getTag(ViewField.DefaultValueTagKey)).toReturn("file://foo/bar.jpg")
    field.getterFromItem(List(OperationResponse(TheViewId, null), outerView)) must be (Some(Uri.parse("file://foo/bar.jpg")))
  }
}
