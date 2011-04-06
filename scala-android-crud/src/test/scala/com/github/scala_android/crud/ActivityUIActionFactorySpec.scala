package com.github.scala_android.crud

import _root_.android.content.Intent
import org.junit.Test
import org.junit.runner.RunWith
import com.xtremelabs.robolectric.RobolectricTestRunner
import org.scalatest.matchers.ShouldMatchers

/**
 * A test for {@link CrudListActivity}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/18/11
 * Time: 6:22 PM
 */
@RunWith(classOf[RobolectricTestRunner])
class ActivityUIActionFactorySpec extends MyEntityTesting with ShouldMatchers {
  //todo determine if shadowing, and run tests on real Android device as well.
  val isShadowing = true

  import ActivityUIActionFactory._
  import MyCrudEntityTypeRef.entityName

  val crudContext = new CrudContext(null)

  @Test
  def getCreateIntentShouldGetTheRightUri {
    getCreateIntent(MyCrudEntityTypeRef, toUri("foo"), crudContext).getData should
      be (toUri("foo", entityName))
    getCreateIntent(MyCrudEntityTypeRef, toUri("foo", entityName), crudContext).getData should
      be (toUri("foo", entityName))
    getCreateIntent(MyCrudEntityTypeRef, toUri("foo", entityName, "123"), crudContext).getData should
      be (toUri("foo", entityName))
    getCreateIntent(MyCrudEntityTypeRef, toUri("foo", entityName, "123", "bar"), crudContext).getData should
      be (toUri("foo", entityName))
    getCreateIntent(MyCrudEntityTypeRef, toUri(), crudContext).getData should
      be (toUri(entityName))
  }

  @Test
  def getDisplayListIntentShouldGetTheRightUri {
    getDisplayListIntent(MyCrudEntityTypeRef, toUri("foo"), crudContext).getData should
      be (toUri("foo", entityName))
    getDisplayListIntent(MyCrudEntityTypeRef, toUri("foo", entityName), crudContext).getData should
      be (toUri("foo", entityName))
    getDisplayListIntent(MyCrudEntityTypeRef, toUri("foo", entityName, "123"), crudContext).getData should
      be (toUri("foo", entityName))
    getDisplayListIntent(MyCrudEntityTypeRef, toUri("foo", entityName, "123", "bar"), crudContext).getData should
      be (toUri("foo", entityName))
    getDisplayListIntent(MyCrudEntityTypeRef, toUri(), crudContext).getData should
      be (toUri(entityName))
  }

  @Test
  def getDisplayListIntentWithUriContextShouldGetTheRightUri {
    getDisplayListIntent(MyCrudEntityTypeRef, toUri("foo"), None, crudContext).getData should
      be (toUri("foo", entityName))
    getDisplayListIntent(MyCrudEntityTypeRef, toUri("foo"), Some(EntityUriSegment("bar")), crudContext).getData should
      be (toUri("foo", "bar", entityName))
    getDisplayListIntent(MyCrudEntityTypeRef, toUri("foo"), Some(EntityUriSegment("bar", "123")), crudContext).getData should
      be (toUri("foo", "bar", "123", entityName))
    getDisplayListIntent(MyCrudEntityTypeRef, toUri("foo", "bar", "234", entityName), Some(EntityUriSegment("bar", "123")), crudContext).getData should
      be (toUri("foo", "bar", "123", entityName))
  }

  @Test
  def getDisplayIntentShouldGetTheRightUri {
    getDisplayIntent(MyCrudEntityTypeRef, 35, toUri("foo"), crudContext).getData should
      be (toUri("foo", entityName, "35"))
    getDisplayIntent(MyCrudEntityTypeRef, 34, toUri("foo", entityName), crudContext).getData should
      be (toUri("foo", entityName, "34"))
    getDisplayIntent(MyCrudEntityTypeRef, 34, toUri("foo", entityName, "123"), crudContext).getData should
      be (toUri("foo", entityName, "34"))
    getDisplayIntent(MyCrudEntityTypeRef, 34, toUri("foo", entityName, "123", "bar"), crudContext).getData should
      be (toUri("foo", entityName, "34"))
    getDisplayIntent(MyCrudEntityTypeRef, 34, toUri(), crudContext).getData should
      be (toUri(entityName, "34"))
  }

  @Test
  def getUpdateIntentShouldGetTheRightUri {
    getUpdateIntent(MyCrudEntityTypeRef, 35, toUri("foo"), crudContext).getData should
      be (toUri("foo", entityName, "35"))
    getUpdateIntent(MyCrudEntityTypeRef, 34, toUri("foo", entityName), crudContext).getData should
      be (toUri("foo", entityName, "34"))
    getUpdateIntent(MyCrudEntityTypeRef, 34, toUri("foo", entityName, "123"), crudContext).getData should
      be (toUri("foo", entityName, "34"))
    getUpdateIntent(MyCrudEntityTypeRef, 34, toUri("foo", entityName, "123", "bar"), crudContext).getData should
      be (toUri("foo", entityName, "34"))
    getUpdateIntent(MyCrudEntityTypeRef, 34, toUri(), crudContext).getData should
      be (toUri(entityName, "34"))
  }

  @Test
  def deleteActionShouldBeUndoable {
    val currentActivity = mock[CrudActivity[_,_,_,_]]
    val application = mock[CrudApplication]
    val entityType = mock[CrudEntityType[_,_,_,_]]
    val id = 345L
    val uiFactory = new ActivityUIActionFactory(currentActivity, application)
    expecting {
      call(entityType.deleteItemString).andReturn(5)
      call(entityType.startDelete(id, uiFactory))
    }
    whenExecuting(entityType, currentActivity, application) {
      uiFactory.startDelete(entityType)(id)
    }
  }

  @Test
  def shouldGetTheRightAction {
    if (!isShadowing) {
      getCreateIntent(MyCrudEntityTypeRef, toUri("foo"), crudContext).getAction should be (Intent.ACTION_INSERT)
      getDisplayListIntent(MyCrudEntityTypeRef, toUri("foo"), crudContext).getAction should be (Intent.ACTION_PICK)
      getDisplayIntent(MyCrudEntityTypeRef, 45, toUri("foo", entityName), crudContext).getAction should be (Intent.ACTION_VIEW)
      getUpdateIntent(MyCrudEntityTypeRef, 45, toUri("foo", entityName), crudContext).getAction should be (Intent.ACTION_EDIT)
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