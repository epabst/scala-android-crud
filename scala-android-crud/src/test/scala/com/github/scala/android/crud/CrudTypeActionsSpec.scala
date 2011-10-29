package com.github.scala.android.crud

import _root_.android.content.Intent
import action.{ContextVars, UriPath}
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito._
import com.xtremelabs.robolectric.RobolectricTestRunner
import org.scalatest.matchers.MustMatchers
import com.github.scala.android.crud.action.Action.toRichItent

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

  import MyCrudType.entityName

  @Test
  def createActionShouldHaveTheRightUri() {
    val activity = null
    MyCrudType.createAction.get.determineIntent(UriPath("foo"), activity).uriPath must
      be (UriPath("foo", entityName))
    MyCrudType.createAction.get.determineIntent(UriPath("foo", entityName), activity).uriPath must
      be (UriPath("foo", entityName))
    MyCrudType.createAction.get.determineIntent(UriPath("foo", entityName, "123"), activity).uriPath must
      be (UriPath("foo", entityName))
    MyCrudType.createAction.get.determineIntent(UriPath("foo", entityName, "123", "bar"), activity).uriPath must
      be (UriPath("foo", entityName))
    MyCrudType.createAction.get.determineIntent(UriPath(), activity).uriPath must
      be (UriPath(entityName))
  }

  @Test
  def listActionShouldHaveTheRightUri() {
    val activity = null
    MyCrudType.listAction.determineIntent(UriPath("foo"), activity).uriPath must
      be (UriPath("foo", entityName))
    MyCrudType.listAction.determineIntent(UriPath("foo", entityName), activity).uriPath must
      be (UriPath("foo", entityName))
    MyCrudType.listAction.determineIntent(UriPath("foo", entityName, "123"), activity).uriPath must
      be (UriPath("foo", entityName))
    MyCrudType.listAction.determineIntent(UriPath("foo", entityName, "123", "bar"), activity).uriPath must
      be (UriPath("foo", entityName))
    MyCrudType.listAction.determineIntent(UriPath(), activity).uriPath must
      be (UriPath(entityName))
  }

  @Test
  def displayActionShouldHaveTheRightUri() {
    val activity = null
    MyCrudType.displayAction.determineIntent(UriPath("foo", entityName, "35"), activity).uriPath must
      be (UriPath("foo", entityName, "35"))
    MyCrudType.displayAction.determineIntent(UriPath("foo", entityName, "34", "bar"), activity).uriPath must
      be (UriPath("foo", entityName, "34"))
    MyCrudType.displayAction.determineIntent(UriPath("foo", entityName, "34", "bar", "123"), activity).uriPath must
      be (UriPath("foo", entityName, "34"))
  }

  @Test
  def updateActionShouldHaveTheRightUri() {
    val activity = null
    MyCrudType.updateAction.get.determineIntent(UriPath("foo", entityName, "35"), activity).uriPath must
      be (UriPath("foo", entityName, "35"))
    MyCrudType.updateAction.get.determineIntent(UriPath("foo", entityName, "34", "bar"), activity).uriPath must
      be (UriPath("foo", entityName, "34"))
    MyCrudType.updateAction.get.determineIntent(UriPath("foo", entityName, "34", "bar", "123"), activity).uriPath must
      be (UriPath("foo", entityName, "34"))
  }

  @Test
  def deleteActionShouldBeUndoable() {
    val currentActivity = mock[CrudActivity]
    val crudContext = mock[CrudContext]
    stub(currentActivity.crudContext).toReturn(crudContext)
    stub(crudContext.vars).toReturn(new ContextVars {})
    val persistence = mock[CrudPersistence]
    val entityType = new MyCrudType {
      override def newWritable = Unit
      override protected def createEntityPersistence(crudContext: CrudContext) = persistence
    }
    val id = 345L
    stub(persistence.find(id)).toReturn(Some(Unit))
    entityType.deleteAction.get.invoke(UriPath(entityType.entityName, id.toString), currentActivity)
  }

  @Test
  def shouldHaveTheStandardActionNames() {
    if (!isShadowing) {
      val activity = null
      MyCrudType.createAction.get.determineIntent(UriPath("foo"), activity).getAction must be (Intent.ACTION_INSERT)
      MyCrudType.listAction.determineIntent(UriPath("foo"), activity).getAction must be (Intent.ACTION_PICK)
      MyCrudType.displayAction.determineIntent(UriPath("foo"), activity).getAction must be (Intent.ACTION_VIEW)
      MyCrudType.updateAction.get.determineIntent(UriPath("foo"), activity).getAction must be (Intent.ACTION_EDIT)
    }
  }
}