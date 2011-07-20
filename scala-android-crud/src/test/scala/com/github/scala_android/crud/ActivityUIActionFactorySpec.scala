package com.github.scala_android.crud

import _root_.android.content.Intent
import org.junit.Test
import org.junit.runner.RunWith
import com.xtremelabs.robolectric.RobolectricTestRunner
import org.scalatest.matchers.MustMatchers

/**
 * A test for {@link CrudListActivity}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/18/11
 * Time: 6:22 PM
 */
@RunWith(classOf[RobolectricTestRunner])
class ActivityUIActionFactorySpec extends MyEntityTesting with MustMatchers {
  //todo determine if shadowing, and run tests on real Android device as well.
  val isShadowing = true

  import ActivityUIActionFactory._
  import MyCrudType.entityName

  val crudContext = new CrudContext(null, null)

  @Test
  def getCreateIntentShouldGetTheRightUri {
    getCreateIntent(MyCrudType, toUri("foo"), crudContext).getData must
      be (toUri("foo", entityName))
    getCreateIntent(MyCrudType, toUri("foo", entityName), crudContext).getData must
      be (toUri("foo", entityName))
    getCreateIntent(MyCrudType, toUri("foo", entityName, "123"), crudContext).getData must
      be (toUri("foo", entityName))
    getCreateIntent(MyCrudType, toUri("foo", entityName, "123", "bar"), crudContext).getData must
      be (toUri("foo", entityName))
    getCreateIntent(MyCrudType, toUri(), crudContext).getData must
      be (toUri(entityName))
  }

  @Test
  def getDisplayListIntentShouldGetTheRightUri {
    getDisplayListIntent(MyCrudType, toUri("foo"), crudContext).getData must
      be (toUri("foo", entityName))
    getDisplayListIntent(MyCrudType, toUri("foo", entityName), crudContext).getData must
      be (toUri("foo", entityName))
    getDisplayListIntent(MyCrudType, toUri("foo", entityName, "123"), crudContext).getData must
      be (toUri("foo", entityName))
    getDisplayListIntent(MyCrudType, toUri("foo", entityName, "123", "bar"), crudContext).getData must
      be (toUri("foo", entityName))
    getDisplayListIntent(MyCrudType, toUri(), crudContext).getData must
      be (toUri(entityName))
  }

  @Test
  def getDisplayListIntentWithUriContextShouldGetTheRightUri {
    getDisplayListIntent(MyCrudType, toUri("foo"), None, crudContext).getData must
      be (toUri("foo", entityName))
    getDisplayListIntent(MyCrudType, toUri("foo"), Some(EntityUriSegment("bar")), crudContext).getData must
      be (toUri("foo", "bar", entityName))
    getDisplayListIntent(MyCrudType, toUri("foo"), Some(EntityUriSegment("bar", "123")), crudContext).getData must
      be (toUri("foo", "bar", "123", entityName))
    getDisplayListIntent(MyCrudType, toUri("foo", "bar", "234", entityName), Some(EntityUriSegment("bar", "123")), crudContext).getData must
      be (toUri("foo", "bar", "123", entityName))
  }

  @Test
  def getDisplayIntentShouldGetTheRightUri {
    getDisplayIntent(MyCrudType, 35, toUri("foo"), crudContext).getData must
      be (toUri("foo", entityName, "35"))
    getDisplayIntent(MyCrudType, 34, toUri("foo", entityName), crudContext).getData must
      be (toUri("foo", entityName, "34"))
    getDisplayIntent(MyCrudType, 34, toUri("foo", entityName, "123"), crudContext).getData must
      be (toUri("foo", entityName, "34"))
    getDisplayIntent(MyCrudType, 34, toUri("foo", entityName, "123", "bar"), crudContext).getData must
      be (toUri("foo", entityName, "34"))
    getDisplayIntent(MyCrudType, 34, toUri(), crudContext).getData must
      be (toUri(entityName, "34"))
  }

  @Test
  def getUpdateIntentShouldGetTheRightUri {
    getUpdateIntent(MyCrudType, 35, toUri("foo"), crudContext).getData must
      be (toUri("foo", entityName, "35"))
    getUpdateIntent(MyCrudType, 34, toUri("foo", entityName), crudContext).getData must
      be (toUri("foo", entityName, "34"))
    getUpdateIntent(MyCrudType, 34, toUri("foo", entityName, "123"), crudContext).getData must
      be (toUri("foo", entityName, "34"))
    getUpdateIntent(MyCrudType, 34, toUri("foo", entityName, "123", "bar"), crudContext).getData must
      be (toUri("foo", entityName, "34"))
    getUpdateIntent(MyCrudType, 34, toUri(), crudContext).getData must
      be (toUri(entityName, "34"))
  }

  @Test
  def deleteActionShouldBeUndoable {
    val currentActivity = mock[CrudActivity]
    val application = mock[CrudApplication]
    val entityType = mock[CrudType]
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
      getCreateIntent(MyCrudType, toUri("foo"), crudContext).getAction must be (Intent.ACTION_INSERT)
      getDisplayListIntent(MyCrudType, toUri("foo"), crudContext).getAction must be (Intent.ACTION_PICK)
      getDisplayIntent(MyCrudType, 45, toUri("foo", entityName), crudContext).getAction must be (Intent.ACTION_VIEW)
      getUpdateIntent(MyCrudType, 45, toUri("foo", entityName), crudContext).getAction must be (Intent.ACTION_EDIT)
    }
  }

  @Test
  def segmentShouldFindId {
    EntityUriSegment(entityName).findId(toUri("foo")) must be (None)
    EntityUriSegment(entityName).findId(toUri(entityName)) must be (None)
    EntityUriSegment(entityName).findId(toUri(entityName, "123")) must be (Some(123))
    EntityUriSegment(entityName).findId(toUri(entityName, "123", "foo")) must be (Some(123))
    EntityUriSegment(entityName).findId(toUri(entityName, "blah")) must be (None)
  }
}