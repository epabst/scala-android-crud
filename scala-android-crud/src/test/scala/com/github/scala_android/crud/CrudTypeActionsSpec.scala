package com.github.scala_android.crud

import _root_.android.content.Intent
import action.UriPath
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito._
import com.xtremelabs.robolectric.RobolectricTestRunner
import org.scalatest.matchers.MustMatchers
import android.net.Uri

/**
 * A test for {@link CrudListActivity}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/18/11
 * Time: 6:22 PM
 */
@RunWith(classOf[RobolectricTestRunner])
class CrudTypeActionsSpec extends MyEntityTesting with MustMatchers with CrudMockitoSugar {
  //todo determine if shadowing, and run tests on real Android device as well.
  val isShadowing = true

  import com.github.scala_android.crud.action.Action.toUri
  import MyCrudType.entityName

  val crudContext = new CrudContext(null, null)

  @Test
  def createActionShouldHaveTheRightUri() {
    val activity = null
    MyCrudType.createAction.determineIntent(toUri("foo"), activity).getData must
      be (toUri("foo", entityName))
    MyCrudType.createAction.determineIntent(toUri("foo", entityName), activity).getData must
      be (toUri("foo", entityName))
    MyCrudType.createAction.determineIntent(toUri("foo", entityName, "123"), activity).getData must
      be (toUri("foo", entityName))
    MyCrudType.createAction.determineIntent(toUri("foo", entityName, "123", "bar"), activity).getData must
      be (toUri("foo", entityName))
    MyCrudType.createAction.determineIntent(toUri(), activity).getData must
      be (toUri(entityName))
  }

  @Test
  def listActionShouldHaveTheRightUri() {
    val activity = null
    MyCrudType.listAction.determineIntent(toUri("foo"), activity).getData must
      be (toUri("foo", entityName))
    MyCrudType.listAction.determineIntent(toUri("foo", entityName), activity).getData must
      be (toUri("foo", entityName))
    MyCrudType.listAction.determineIntent(toUri("foo", entityName, "123"), activity).getData must
      be (toUri("foo", entityName))
    MyCrudType.listAction.determineIntent(toUri("foo", entityName, "123", "bar"), activity).getData must
      be (toUri("foo", entityName))
    MyCrudType.listAction.determineIntent(toUri(), activity).getData must
      be (toUri(entityName))
  }

  @Test
  def displayActionShouldHaveTheRightUri() {
    val activity = null
    MyCrudType.displayAction.determineIntent(toUri("foo", entityName, "35"), activity).getData must
      be (toUri("foo", entityName, "35"))
    MyCrudType.displayAction.determineIntent(toUri("foo", entityName, "34", "bar"), activity).getData must
      be (toUri("foo", entityName, "34"))
    MyCrudType.displayAction.determineIntent(toUri("foo", entityName, "34", "bar", "123"), activity).getData must
      be (toUri("foo", entityName, "34"))
  }

  @Test
  def updateActionShouldHaveTheRightUri() {
    val activity = null
    MyCrudType.updateAction.determineIntent(toUri("foo", entityName, "35"), activity).getData must
      be (toUri("foo", entityName, "35"))
    MyCrudType.updateAction.determineIntent(toUri("foo", entityName, "34", "bar"), activity).getData must
      be (toUri("foo", entityName, "34"))
    MyCrudType.updateAction.determineIntent(toUri("foo", entityName, "34", "bar", "123"), activity).getData must
      be (toUri("foo", entityName, "34"))
  }

  @Test
  def deleteActionShouldBeUndoable() {
    val currentActivity = mock[CrudActivity]
    val persistence = mock[CrudPersistence]
    val entityType = new MyCrudType {
      override def newWritable = Unit
      override def openEntityPersistence(crudContext: CrudContext) = persistence
    }
    val id = 345L
    stub(persistence.find(id)).toReturn(Some(Unit))
    entityType.deleteAction.invoke(UriPath(entityType.entityName, id.toString).specifyInUri(Uri.EMPTY), currentActivity)
  }

  @Test
  def shouldHaveTheStandardActionNames() {
    if (!isShadowing) {
      val activity = null
      MyCrudType.createAction.determineIntent(toUri("foo"), activity).getAction must be (Intent.ACTION_INSERT)
      MyCrudType.listAction.determineIntent(toUri("foo"), activity).getAction must be (Intent.ACTION_PICK)
      MyCrudType.displayAction.determineIntent(toUri("foo"), activity).getAction must be (Intent.ACTION_VIEW)
      MyCrudType.updateAction.determineIntent(toUri("foo"), activity).getAction must be (Intent.ACTION_EDIT)
    }
  }
}