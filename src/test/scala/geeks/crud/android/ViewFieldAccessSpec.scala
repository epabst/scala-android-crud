package geeks.crud.android

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.Spec
import java.text.NumberFormat
import geeks.crud.Field
import geeks.crud.Field._
import geeks.crud.android.CursorFieldAccess._
import geeks.crud.android.ViewFieldAccess._
import _root_.android.content.Context
import android.view.{ViewGroup, View}
import org.scalatest.mock.EasyMockSugar
import android.widget.{Spinner, LinearLayout, TextView}
import com.xtremelabs.robolectric.RobolectricTestRunner
import org.junit.Test


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
      persisted[String]("name"),
      viewId[TextView,String](101),
      viewFieldAccessById[MyView,String](102, _.status, _.status_=),
      viewId(102)(viewFieldAccess[MyView,String](_.status, _.status_=)))
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
        fieldAccess[MyEntity,String](_.string, _.string_=),
        viewFieldAccessById[Spinner,String](56,
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
      val stringField = Field[String](
        fieldAccess[MyEntity,String](_.string, _.string_=),
        viewFieldAccessById[Spinner,String](56,
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
}