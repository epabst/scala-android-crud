package com.github.scala.android.crud

import _root_.android.content.Intent
import action.{StartActivityOperation, Action, UriPath}
import org.junit.Test
import org.junit.runner.RunWith
import com.xtremelabs.robolectric.RobolectricTestRunner
import org.scalatest.matchers.MustMatchers
import com.github.scala.android.crud.action.Operation.toRichItent

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

  val Action(_, createOperation: StartActivityOperation) = MyCrudType.createAction.get
  val Action(_, listOperation: StartActivityOperation) = MyCrudType.listAction
  val Action(_, displayOperation: StartActivityOperation) = MyCrudType.displayAction
  val Action(_, updateOperation: StartActivityOperation) = MyCrudType.updateAction.get

  @Test
  def createActionShouldHaveTheRightUri() {
    val activity = null
    createOperation.determineIntent(UriPath("foo"), activity).uriPath must
      be (UriPath("foo", entityName))
    createOperation.determineIntent(UriPath("foo", entityName), activity).uriPath must
      be (UriPath("foo", entityName))
    createOperation.determineIntent(UriPath("foo", entityName, "123"), activity).uriPath must
      be (UriPath("foo", entityName))
    createOperation.determineIntent(UriPath("foo", entityName, "123", "bar"), activity).uriPath must
      be (UriPath("foo", entityName))
    createOperation.determineIntent(UriPath(), activity).uriPath must
      be (UriPath(entityName))
  }

  @Test
  def listActionShouldHaveTheRightUri() {
    val activity = null
    listOperation.determineIntent(UriPath("foo"), activity).uriPath must
      be (UriPath("foo", entityName))
    listOperation.determineIntent(UriPath("foo", entityName), activity).uriPath must
      be (UriPath("foo", entityName))
    listOperation.determineIntent(UriPath("foo", entityName, "123"), activity).uriPath must
      be (UriPath("foo", entityName))
    listOperation.determineIntent(UriPath("foo", entityName, "123", "bar"), activity).uriPath must
      be (UriPath("foo", entityName))
    listOperation.determineIntent(UriPath(), activity).uriPath must
      be (UriPath(entityName))
  }

  @Test
  def displayActionShouldHaveTheRightUri() {
    val activity = null
    displayOperation.determineIntent(UriPath("foo", entityName, "35"), activity).uriPath must
      be (UriPath("foo", entityName, "35"))
    displayOperation.determineIntent(UriPath("foo", entityName, "34", "bar"), activity).uriPath must
      be (UriPath("foo", entityName, "34"))
    displayOperation.determineIntent(UriPath("foo", entityName, "34", "bar", "123"), activity).uriPath must
      be (UriPath("foo", entityName, "34"))
  }

  @Test
  def updateActionShouldHaveTheRightUri() {
    val activity = null
    updateOperation.determineIntent(UriPath("foo", entityName, "35"), activity).uriPath must
      be (UriPath("foo", entityName, "35"))
    updateOperation.determineIntent(UriPath("foo", entityName, "34", "bar"), activity).uriPath must
      be (UriPath("foo", entityName, "34"))
    updateOperation.determineIntent(UriPath("foo", entityName, "34", "bar", "123"), activity).uriPath must
      be (UriPath("foo", entityName, "34"))
  }

  @Test
  def shouldHaveTheStandardActionNames() {
    if (!isShadowing) {
      val activity = null
      createOperation.determineIntent(UriPath("foo"), activity).getAction must be (Intent.ACTION_INSERT)
      listOperation.determineIntent(UriPath("foo"), activity).getAction must be (Intent.ACTION_PICK)
      displayOperation.determineIntent(UriPath("foo"), activity).getAction must be (Intent.ACTION_VIEW)
      updateOperation.determineIntent(UriPath("foo"), activity).getAction must be (Intent.ACTION_EDIT)
    }
  }
}