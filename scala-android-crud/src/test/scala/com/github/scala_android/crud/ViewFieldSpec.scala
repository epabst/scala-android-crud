package com.github.scala_android.crud

import org.junit.runner.RunWith
import org.scalatest.matchers.ShouldMatchers
import com.github.triangle.PortableField
import com.github.triangle.PortableField._
import com.github.scala_android.crud.CursorField._
import com.github.scala_android.crud.ViewField._
import android.view.View
import org.scalatest.mock.EasyMockSugar
import android.widget.{Spinner, LinearLayout, TextView}
import com.xtremelabs.robolectric.RobolectricTestRunner
import org.junit.Test
import android.content.{Intent, Context}


/**
 * A behavior specification for {@link ViewField}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/9/11
 * Time: 7:59 PM
 */

@RunWith(classOf[RobolectricTestRunner])
class ViewFieldSpec extends ShouldMatchers with EasyMockSugar {
  class MyEntity(var string: String, var number: Int)

  @Test
  def itShouldBeEasilyInstantiableForAView() {
    class MyView(context: Context, var status: String) extends View(context)

    val stringField =
      persisted[String]("name") +
      viewId(101, textView) +
      viewId(102, viewField[MyView,String](_.status, _.status_=))
  }

  @Test
  def itShouldClearTheViewIfEmpty() {
    val viewGroup = mock[View]
    val view1 = mock[TextView]
    val view2 = mock[TextView]
    val view3 = mock[TextView]
    expecting {
      call(viewGroup.findViewById(101)).andReturn(view1).anyTimes
      call(view1.setText(""))
      call(viewGroup.findViewById(102)).andReturn(view2).anyTimes
      call(view2.setText("Please Fill"))
      call(viewGroup.findViewById(103)).andReturn(view3).anyTimes
      call(view3.setText(""))
    }
    whenExecuting(viewGroup, view1, view2, view3) {
      val stringField =
        viewId(101, textView) +
        viewId(102, viewField[TextView,String](v => Option(v.getText.toString), _.setText, _.setText("Please Fill")))
      stringField.setValue(viewGroup, None)

      val intField = viewId(103, formatted[Int](textView))
      intField.setValue(viewGroup, None)
    }
  }

  @Test
  def itShouldOnlyCopyToAndFromViewByIdIfTheRightType() {
    val context = mock[Context]
    val group = mock[View]
    val view = mock[TextView]
    expecting {
      call(group.findViewById(56)).andReturn(view).anyTimes
    }
    whenExecuting(context, group, view) {
      val stringField = field[MyEntity,String](_.string, _.string_=) +
        viewId(56, viewField[Spinner,String](
          _ => throw new IllegalStateException("should not be called"),
          _ => throw new IllegalStateException("should not be called")))
      val myEntity1 = new MyEntity("my1", 1)
      stringField.copy(myEntity1, group) should be (false)
      stringField.copy(group, myEntity1) should be (false)
    }
  }

  @Test
  def itShouldOnlyCopyToAndFromViewByIdIfIdIsFound() {
    val context = mock[Context]
    whenExecuting(context) {
      val stringField = field[MyEntity,String](_.string, _.string_=) +
        viewId(56, viewField[Spinner,String](
          _ => throw new IllegalStateException("should not be called"),
          _ => throw new IllegalStateException("should not be called")))
      val myEntity1 = new MyEntity("my1", 1)
      val group = new LinearLayout(context)
      val view = new Spinner(context)
      view.setId(100)
      group.addView(view)
      stringField.copy(myEntity1, group) should be (false)
      stringField.copy(group, myEntity1) should be (false)
    }
  }

  @Test
  def itShouldHandleUnparseableValues() {
    val context = mock[Context]
    whenExecuting(context) {
      val intField = formatted[Int](textView) + field[MyEntity,Int](_.number, _.number_=)
      val view = new TextView(context)
      view.setText("twenty")
      intField.getter(view) should be (None)

      val entity = new MyEntity("my1", 30)
      val result = intField.copy(view, entity)
      result should be (true)
      entity.number should be (30)
    }
  }

  @Test
  def itShouldGetTheIdForAnEntityNameFromTheIntent() {
    val field = intentId("foo")
    val intent = new Intent(null, ActivityUIActionFactory.toUri("hello", "1", "foo", "4", "bar", "3"))
    field.getter(intent) should be (Some(4))

    val intent2 = new Intent(null, ActivityUIActionFactory.toUri("hello", "1", "foo"))
    field.getter(intent2) should be (None)

    val intent3 = new Intent(null, ActivityUIActionFactory.toUri("hello"))
    field.getter(intent3) should be (None)

    val intent4 = new Intent(null, ActivityUIActionFactory.toUri())
    field.getter(intent4) should be (None)

    val intent5 = new Intent(null, ActivityUIActionFactory.toUri("4"))
    field.getter(intent5) should be (None)
  }

  @Test
  def itShouldConvertNullToNone() {
    val context = mock[Context]
    whenExecuting(context) {
      val field = textView
      val view = new TextView(context)
      view.setText(null)
      field.getter(view) should be (None)

      view.setText("")
      field.getter(view) should be (None)
    }
  }

  @Test
  def itShouldTrimStrings() {
    val context = mock[Context]
    whenExecuting(context) {
      val field = textView
      val view = new TextView(context)
      view.setText("  ")
      field.getter(view) should be (None)

      view.setText(" hello world ")
      field.getter(view) should be (Some("hello world"))
    }
  }
}