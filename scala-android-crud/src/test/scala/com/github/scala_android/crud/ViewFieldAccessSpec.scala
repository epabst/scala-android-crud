package com.github.scala_android.crud

import org.junit.runner.RunWith
import org.scalatest.matchers.ShouldMatchers
import com.github.triangle.Field
import com.github.triangle.Field._
import com.github.scala_android.crud.CursorFieldAccess._
import com.github.scala_android.crud.ViewFieldAccess._
import android.view.View
import org.scalatest.mock.EasyMockSugar
import android.widget.{Spinner, LinearLayout, TextView}
import com.xtremelabs.robolectric.RobolectricTestRunner
import org.junit.Test
import android.content.{Intent, Context}


/**
 * A behavior specification for {@link ViewFieldAccess}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/9/11
 * Time: 7:59 PM
 */

@RunWith(classOf[RobolectricTestRunner])
class ViewFieldAccessSpec extends ShouldMatchers with EasyMockSugar {
  class MyEntity(var string: String, var number: Int)

  @Test
  def itShouldBeEasilyInstantiableForAView() {
    class MyView(context: Context, var status: String) extends View(context)

    val stringField = Field(
      persisted[String]("name") +
      viewId(101, textView) +
      viewId(102, viewFieldAccess[MyView,String](_.status, _.status_=)))
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
      val stringField = Field(
        viewId(101, textView) +
        viewId(102, viewFieldAccess[TextView,String](v => Option(v.getText.toString), _.setText, _.setText("Please Fill"))))
      stringField.setValue(viewGroup, None)

      val intField = Field(viewId(103, formatted[Int](textView)))
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
      val stringField = Field[String](
        fieldAccess[MyEntity,String](_.string, _.string_=) +
        viewId(56, viewFieldAccess[Spinner,String](
          _ => throw new IllegalStateException("should not be called"),
          _ => throw new IllegalStateException("should not be called"))))
      val myEntity1 = new MyEntity("my1", 1)
      stringField.copy(myEntity1, group) should be (false)
      stringField.copy(group, myEntity1) should be (false)
    }
  }

  @Test
  def itShouldOnlyCopyToAndFromViewByIdIfIdIsFound() {
    val context = mock[Context]
    whenExecuting(context) {
      val stringField = Field[String](
        fieldAccess[MyEntity,String](_.string, _.string_=) +
        viewId(56, viewFieldAccess[Spinner,String](
          _ => throw new IllegalStateException("should not be called"),
          _ => throw new IllegalStateException("should not be called"))))
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
      val field = Field(formatted[Int](textView) + fieldAccess[MyEntity,Int](_.number, _.number_=))
      val view = new TextView(context)
      view.setText("twenty")
      field.findValue(view) should be (None)
      field.getter(view) should be (None)

      val entity = new MyEntity("my1", 30)
      val result = field.copy(view, entity)
      result should be (true)
      entity.number should be (30)
    }
  }

  @Test
  def itShouldGetTheIdForAnEntityNameFromTheIntent() {
    val field = Field(intentId("foo"))
    val intent = new Intent(null, ActivityUIActionFactory.toUri("hello", "1", "foo", "4", "bar", "3"))
    field.findValue(intent) should be (Some(4))

    val intent2 = new Intent(null, ActivityUIActionFactory.toUri("hello", "1", "foo"))
    field.findValue(intent2) should be (None)

    val intent3 = new Intent(null, ActivityUIActionFactory.toUri("hello"))
    field.findValue(intent3) should be (None)

    val intent4 = new Intent(null, ActivityUIActionFactory.toUri())
    field.findValue(intent4) should be (None)

    val intent5 = new Intent(null, ActivityUIActionFactory.toUri("4"))
    field.findValue(intent5) should be (None)
  }

  @Test
  def itShouldConvertNullToNone() {
    val context = mock[Context]
    whenExecuting(context) {
      val field = Field(textView)
      val view = new TextView(context)
      view.setText(null)
      field.findValue(view) should be (None)

      view.setText("")
      field.findValue(view) should be (None)
    }
  }

  @Test
  def itShouldTrimStrings() {
    val context = mock[Context]
    whenExecuting(context) {
      val field = Field(textView)
      val view = new TextView(context)
      view.setText("  ")
      field.findValue(view) should be (None)

      view.setText(" hello world ")
      field.findValue(view) should be (Some("hello world"))
    }
  }
}