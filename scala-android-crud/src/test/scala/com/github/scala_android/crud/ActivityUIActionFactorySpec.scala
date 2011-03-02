package com.github.scala_android.crud

import _root_.android.content.Intent
import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.mock.EasyMockSugar
import com.xtremelabs.robolectric.RobolectricTestRunner
import org.scalatest.matchers.ShouldMatchers

/**
 * A test for {@link CrudListActivity}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/18/11
 * Time: 6:22 PM
 */
@RunWith(classOf[RobolectricTestRunner])
class ActivityUIActionFactorySpec extends EasyMockSugar with ShouldMatchers {
  //todo determine if shadowing, and run tests on real Android device as well.
  val isShadowing = true

  object MyEntityType extends CrudEntityType {
    def entityName = "MyEntity"

    def listActivityClass = classOf[CrudListActivity[_,_,_,_]]

    def addItemString = R.string.add_item

    def editItemString = R.string.edit_item

    def cancelItemString = R.string.cancel_item

    def activityClass = classOf[CrudActivity[_,_,_,_]]
  }

  import ActivityUIActionFactory._
  import MyEntityType.entityName

  val context = null

  @Test
  def getCreateIntentShouldGetTheRightUri {
    val intent = new Intent(null, toUri("foo"))
    //sanity check
    intent.getData should be (toUri("foo"))
    getCreateIntent(MyEntityType, new Intent(null, toUri("foo")), context).getData should
      be (toUri("foo", entityName))
    getCreateIntent(MyEntityType, new Intent(null, toUri("foo", entityName)), context).getData should
      be (toUri("foo", entityName))
    getCreateIntent(MyEntityType, new Intent(null, toUri("foo", entityName, "123")), context).getData should
      be (toUri("foo", entityName))
    getCreateIntent(MyEntityType, new Intent(null, toUri("foo", entityName, "123", "bar")), context).getData should
      be (toUri("foo", entityName))
    getCreateIntent(MyEntityType, new Intent(null, toUri()), context).getData should
      be (toUri(entityName))
  }

  @Test
  def getDisplayListIntentShouldGetTheRightUri {
    val intent = new Intent(null, toUri("foo"))
    //sanity check
    intent.getData should be (toUri("foo"))
    getDisplayListIntent(MyEntityType, Unit, new Intent(null, toUri("foo")), context).getData should
      be (toUri("foo", entityName))
    getDisplayListIntent(MyEntityType, Unit, new Intent(null, toUri("foo", entityName)), context).getData should
      be (toUri("foo", entityName))
    getDisplayListIntent(MyEntityType, Unit, new Intent(null, toUri("foo", entityName, "123")), context).getData should
      be (toUri("foo", entityName))
    getDisplayListIntent(MyEntityType, Unit, new Intent(null, toUri("foo", entityName, "123", "bar")), context).getData should
      be (toUri("foo", entityName))
    getDisplayListIntent(MyEntityType, Unit, new Intent(null, toUri()), context).getData should
      be (toUri(entityName))
  }

  @Test
  def getDisplayIntentShouldGetTheRightUri {
    val intent = new Intent(null, toUri("foo"))
    //sanity check
    intent.getData should be (toUri("foo"))
    getDisplayIntent(MyEntityType, 35, new Intent(null, toUri("foo")), context).getData should
      be (toUri("foo", entityName, "35"))
    getDisplayIntent(MyEntityType, 34, new Intent(null, toUri("foo", entityName)), context).getData should
      be (toUri("foo", entityName, "34"))
    getDisplayIntent(MyEntityType, 34, new Intent(null, toUri("foo", entityName, "123")), context).getData should
      be (toUri("foo", entityName, "34"))
    getDisplayIntent(MyEntityType, 34, new Intent(null, toUri("foo", entityName, "123", "bar")), context).getData should
      be (toUri("foo", entityName, "34"))
    getDisplayIntent(MyEntityType, 34, new Intent(null, toUri()), context).getData should
      be (toUri(entityName, "34"))
  }

  @Test
  def getUpdateIntentShouldGetTheRightUri {
    val intent = new Intent(null, toUri("foo"))
    //sanity check
    intent.getData should be (toUri("foo"))
    getUpdateIntent(MyEntityType, 35, new Intent(null, toUri("foo")), context).getData should
      be (toUri("foo", entityName, "35"))
    getUpdateIntent(MyEntityType, 34, new Intent(null, toUri("foo", entityName)), context).getData should
      be (toUri("foo", entityName, "34"))
    getUpdateIntent(MyEntityType, 34, new Intent(null, toUri("foo", entityName, "123")), context).getData should
      be (toUri("foo", entityName, "34"))
    getUpdateIntent(MyEntityType, 34, new Intent(null, toUri("foo", entityName, "123", "bar")), context).getData should
      be (toUri("foo", entityName, "34"))
    getUpdateIntent(MyEntityType, 34, new Intent(null, toUri()), context).getData should
      be (toUri(entityName, "34"))
  }

  @Test
  def getDeleteIntentShouldGetTheRightUri {
    val intent = new Intent(null, toUri("foo"))
    //sanity check
    intent.getData should be (toUri("foo"))
    getDeleteIntent(MyEntityType, List(35), new Intent(null, toUri("foo")), context).getData should
      be (toUri("foo", entityName, "35"))
    getDeleteIntent(MyEntityType, List(35, 34), new Intent(null, toUri("foo", entityName)), context).getData should
      be (toUri("foo", entityName, "35,34"))
    getDeleteIntent(MyEntityType, List(35), new Intent(null, toUri("foo", entityName, "123")), context).getData should
      be (toUri("foo", entityName, "35"))
    getDeleteIntent(MyEntityType, Nil, new Intent(null, toUri("foo", entityName, "123", "bar")), context).getData should
      be (toUri("foo", entityName, ""))
    getDeleteIntent(MyEntityType, List(35), new Intent(null, toUri()), context).getData should
      be (toUri(entityName, "35"))
  }

  @Test
  def shouldGetTheRightAction {
    if (!isShadowing) {
      getCreateIntent(MyEntityType, new Intent(null, toUri("foo")), context).getAction should be (Intent.ACTION_INSERT)
      getDisplayListIntent(MyEntityType, Unit, new Intent(null, toUri("foo")), context).getAction should be (Intent.ACTION_PICK)
      getDisplayIntent(MyEntityType, 45, new Intent(null, toUri("foo", entityName)), context).getAction should be (Intent.ACTION_VIEW)
      getUpdateIntent(MyEntityType, 45, new Intent(null, toUri("foo", entityName)), context).getAction should be (Intent.ACTION_EDIT)
      getDeleteIntent(MyEntityType, List(45), new Intent(null, toUri("foo", entityName)), context).getAction should be (Intent.ACTION_DELETE)
    }
  }
}