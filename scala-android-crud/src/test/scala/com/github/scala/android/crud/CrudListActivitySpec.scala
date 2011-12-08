package com.github.scala.android.crud

import _root_.android.content.Intent
import org.junit.Test
import org.junit.runner.RunWith
import com.xtremelabs.robolectric.RobolectricTestRunner
import com.xtremelabs.robolectric.tester.android.view.TestMenu
import org.scalatest.matchers.MustMatchers
import android.view.{View, ContextMenu}
import org.mockito.Mockito._
import org.mockito.Matchers._

/**
 * A test for {@link CrudListActivity}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/18/11
 * Time: 6:22 PM
 */
@RunWith(classOf[RobolectricTestRunner])
class CrudListActivitySpec extends MustMatchers with CrudMockitoSugar {
  @Test
  def shouldAllowAdding() {
    val persistenceFactory = mock[PersistenceFactory]
    val application = mock[CrudApplication]
    val crudType = new MyCrudType(persistenceFactory)
    val activity = new CrudListActivity(crudType, application)
    activity.setIntent(new Intent(Intent.ACTION_MAIN))
    activity.onCreate(null)
    val menu = new TestMenu(activity)
    activity.onCreateOptionsMenu(menu)
    val item0 = menu.getItem(0)
    item0.getTitle.toString must be ("Add")
    menu.size must be (1)

    activity.onOptionsItemSelected(item0) must be (true)
  }

  @Test
  def shouldHaveCorrectContextMenu() {
    val persistenceFactory = mock[PersistenceFactory]
    val application = mock[CrudApplication]
    val contextMenu = mock[ContextMenu]
    val ignoredView: View = null
    val ignoredMenuInfo: ContextMenu.ContextMenuInfo = null
    stub(application.allEntities).toReturn(Nil)
    val crudType = new MyCrudType(persistenceFactory)
    val activity = new CrudListActivity(crudType, application)
    activity.onCreateContextMenu(contextMenu, ignoredView, ignoredMenuInfo)
    verify(contextMenu).add(0, res.R.string.delete_item, 0, res.R.string.delete_item)
  }

  @Test
  def shouldHandleNoEntityOptions() {
    val persistenceFactory = mock[PersistenceFactory]
    val application = mock[CrudApplication]
    val contextMenu = mock[ContextMenu]
    val ignoredView: View = null
    val ignoredMenuInfo: ContextMenu.ContextMenuInfo = null

    val crudType = new MyCrudType(persistenceFactory) {
      override def getEntityActions(application: CrudApplication) = Nil
    }
    val activity = new CrudListActivity(crudType, application)
    //shouldn't do anything
    activity.onCreateContextMenu(contextMenu, ignoredView, ignoredMenuInfo)
  }

  @Test
  def shouldRefreshOnResume() {
    val persistenceFactory = mock[PersistenceFactory]
    val application = mock[CrudApplication]
    val crudType = new MyCrudType(persistenceFactory)
    class MyCrudListActivity extends CrudListActivity(crudType, application) {
      //make it public for testing
      override def onPause() {
        super.onPause()
      }

      //make it public for testing
      override def onResume() {
        super.onResume()
      }
    }
    val activity = new MyCrudListActivity
    activity.setIntent(new Intent(Intent.ACTION_MAIN))
    activity.onCreate(null)
    activity.onPause()
    verify(persistenceFactory, never()).refreshAfterDataChanged(anyObject())

    activity.onResume()
    verify(persistenceFactory, times(1)).refreshAfterDataChanged(anyObject())
  }

  @Test
  def shouldIgnoreClicksOnHeader() {
    val persistenceFactory = mock[PersistenceFactory]
    val application = mock[CrudApplication]
    val crudType = new MyCrudType(persistenceFactory)
    val activity = new CrudListActivity(crudType, application)
    // should do nothing
    activity.onListItemClick(null, null, -1, -1)
  }
}