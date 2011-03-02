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
    getCreateIntent(MyEntityType, toUri("foo"), context).getData should
      be (toUri("foo", entityName))
    getCreateIntent(MyEntityType, toUri("foo", entityName), context).getData should
      be (toUri("foo", entityName))
    getCreateIntent(MyEntityType, toUri("foo", entityName, "123"), context).getData should
      be (toUri("foo", entityName))
    getCreateIntent(MyEntityType, toUri("foo", entityName, "123", "bar"), context).getData should
      be (toUri("foo", entityName))
    getCreateIntent(MyEntityType, toUri(), context).getData should
      be (toUri(entityName))
  }

  @Test
  def getDisplayListIntentShouldGetTheRightUri {
    getDisplayListIntent(MyEntityType, toUri("foo"), context).getData should
      be (toUri("foo", entityName))
    getDisplayListIntent(MyEntityType, toUri("foo", entityName), context).getData should
      be (toUri("foo", entityName))
    getDisplayListIntent(MyEntityType, toUri("foo", entityName, "123"), context).getData should
      be (toUri("foo", entityName))
    getDisplayListIntent(MyEntityType, toUri("foo", entityName, "123", "bar"), context).getData should
      be (toUri("foo", entityName))
    getDisplayListIntent(MyEntityType, toUri(), context).getData should
      be (toUri(entityName))
  }

  @Test
  def getDisplayListIntentWithUriContextShouldGetTheRightUri {
    getDisplayListIntent(MyEntityType, toUri("foo"), None, context).getData should
      be (toUri("foo", entityName))
    getDisplayListIntent(MyEntityType, toUri("foo"), Some(EntityUriSegment("bar")), context).getData should
      be (toUri("foo", "bar", entityName))
    getDisplayListIntent(MyEntityType, toUri("foo"), Some(EntityUriSegment("bar", "123")), context).getData should
      be (toUri("foo", "bar", "123", entityName))
    getDisplayListIntent(MyEntityType, toUri("foo", "bar", "234", entityName), Some(EntityUriSegment("bar", "123")), context).getData should
      be (toUri("foo", "bar", "123", entityName))
  }

  @Test
  def getDisplayIntentShouldGetTheRightUri {
    getDisplayIntent(MyEntityType, 35, toUri("foo"), context).getData should
      be (toUri("foo", entityName, "35"))
    getDisplayIntent(MyEntityType, 34, toUri("foo", entityName), context).getData should
      be (toUri("foo", entityName, "34"))
    getDisplayIntent(MyEntityType, 34, toUri("foo", entityName, "123"), context).getData should
      be (toUri("foo", entityName, "34"))
    getDisplayIntent(MyEntityType, 34, toUri("foo", entityName, "123", "bar"), context).getData should
      be (toUri("foo", entityName, "34"))
    getDisplayIntent(MyEntityType, 34, toUri(), context).getData should
      be (toUri(entityName, "34"))
  }

  @Test
  def getUpdateIntentShouldGetTheRightUri {
    getUpdateIntent(MyEntityType, 35, toUri("foo"), context).getData should
      be (toUri("foo", entityName, "35"))
    getUpdateIntent(MyEntityType, 34, toUri("foo", entityName), context).getData should
      be (toUri("foo", entityName, "34"))
    getUpdateIntent(MyEntityType, 34, toUri("foo", entityName, "123"), context).getData should
      be (toUri("foo", entityName, "34"))
    getUpdateIntent(MyEntityType, 34, toUri("foo", entityName, "123", "bar"), context).getData should
      be (toUri("foo", entityName, "34"))
    getUpdateIntent(MyEntityType, 34, toUri(), context).getData should
      be (toUri(entityName, "34"))
  }

  @Test
  def getDeleteIntentShouldGetTheRightUri {
    getDeleteIntent(MyEntityType, List(35), toUri("foo"), context).getData should
      be (toUri("foo", entityName, "35"))
    getDeleteIntent(MyEntityType, List(35, 34), toUri("foo", entityName), context).getData should
      be (toUri("foo", entityName, "35,34"))
    getDeleteIntent(MyEntityType, List(35), toUri("foo", entityName, "123"), context).getData should
      be (toUri("foo", entityName, "35"))
    getDeleteIntent(MyEntityType, Nil, toUri("foo", entityName, "123", "bar"), context).getData should
      be (toUri("foo", entityName, ""))
    getDeleteIntent(MyEntityType, List(35), toUri(), context).getData should
      be (toUri(entityName, "35"))
  }

  @Test
  def shouldGetTheRightAction {
    if (!isShadowing) {
      getCreateIntent(MyEntityType, toUri("foo"), context).getAction should be (Intent.ACTION_INSERT)
      getDisplayListIntent(MyEntityType, toUri("foo"), context).getAction should be (Intent.ACTION_PICK)
      getDisplayIntent(MyEntityType, 45, toUri("foo", entityName), context).getAction should be (Intent.ACTION_VIEW)
      getUpdateIntent(MyEntityType, 45, toUri("foo", entityName), context).getAction should be (Intent.ACTION_EDIT)
      getDeleteIntent(MyEntityType, List(45), toUri("foo", entityName), context).getAction should be (Intent.ACTION_DELETE)
    }
  }

  @Test
  def segmentShouldFindId {
    EntityUriSegment(entityName).findId(toUri("foo")) should be (None)
    EntityUriSegment(entityName).findId(toUri(entityName)) should be (None)
    EntityUriSegment(entityName).findId(toUri(entityName, "123")) should be (Some(123))
    EntityUriSegment(entityName).findId(toUri(entityName, "123", "foo")) should be (Some(123))
    EntityUriSegment(entityName).findId(toUri(entityName, "blah")) should be (None)
  }
}