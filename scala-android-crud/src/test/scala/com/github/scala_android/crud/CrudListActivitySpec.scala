package com.github.scala_android.crud

import _root_.android.content.Intent
import android.widget.ListAdapter
import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.mock.EasyMockSugar
import com.xtremelabs.robolectric.RobolectricTestRunner
import persistence.CrudPersistence
import com.xtremelabs.robolectric.tester.android.view.TestMenu
import org.scalatest.matchers.MustMatchers
import android.view.{MenuItem, View, ContextMenu}

/**
 * A test for {@link CrudListActivity}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/18/11
 * Time: 6:22 PM
 */
@RunWith(classOf[RobolectricTestRunner])
class CrudListActivitySpec extends EasyMockSugar with MustMatchers with MyEntityTesting {
  @Test
  def shouldAllowAdding() {
    val persistence = mock[CrudPersistence]
    val application = mock[CrudApplication]
    val listAdapter = mock[ListAdapter]
    whenExecuting(persistence, listAdapter, application) {
      val entityType = new MyEntityType(persistence, listAdapter)
      val activity = new CrudListActivity(entityType, application)
      activity.setIntent(new Intent(Intent.ACTION_MAIN))
      activity.onCreate(null)
      val menu = new TestMenu(activity)
      activity.onCreateOptionsMenu(menu)
      val item0 = menu.getItem(0)
      item0.getTitle.toString must be ("Add")
      menu.size must be (1)

      activity.onOptionsItemSelected(item0) must be (true)
    }
  }

  @Test
  def shouldHaveCorrectContextMenu() {
    val persistence = mock[CrudPersistence]
    val application = mock[CrudApplication]
    val contextMenu = mock[ContextMenu]
    val listAdapter = mock[ListAdapter]
    val menuItem = mock[MenuItem]
    val ignoredView: View = null
    val ignoredMenuInfo: ContextMenu.ContextMenuInfo = null
    expecting {
      call(application.allEntities).andReturn(Nil).anyTimes
      call(contextMenu.add(0, 0, 0, res.R.string.delete_item)).andReturn(menuItem)
    }
    whenExecuting(contextMenu, menuItem, application, persistence, listAdapter) {
      val entityType = new MyEntityType(persistence, listAdapter)
      val activity = new CrudListActivity(entityType, application)
      activity.onCreateContextMenu(contextMenu, ignoredView, ignoredMenuInfo)
    }
  }

  @Test
  def shouldHandleNoEntityOptions() {
    val persistence = mock[CrudPersistence]
    val application = mock[CrudApplication]
    val contextMenu = mock[ContextMenu]
    val listAdapter = mock[ListAdapter]
    val ignoredView: View = null
    val ignoredMenuInfo: ContextMenu.ContextMenuInfo = null
    whenExecuting(contextMenu, persistence, application, listAdapter) {
      val entityType = new MyEntityType(persistence, listAdapter) {
        override def getEntityActions(actionFactory: UIActionFactory) = Nil
      }
      val activity = new CrudListActivity(entityType, application)
      //shouldn't do anything
      activity.onCreateContextMenu(contextMenu, ignoredView, ignoredMenuInfo)
    }
  }

  @Test
  def shouldRefreshOnResume() {
    val persistence = mock[CrudPersistence]
    val application = mock[CrudApplication]
    val listAdapter = mock[ListAdapter]
    whenExecuting(persistence, listAdapter, application) {
      val entityType = new MyEntityType(persistence, listAdapter)
      class MyCrudListActivity extends CrudListActivity(entityType, application) {
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
      entityType.refreshCount must be (0)

      activity.onResume()
      entityType.refreshCount must be (1)
    }
  }

  @Test
  def shouldIgnoreClicksOnHeader() {
    val persistence = mock[CrudPersistence]
    val application = mock[CrudApplication]
    val listAdapter = mock[ListAdapter]
    whenExecuting(persistence, application) {
      val entityType = new MyEntityType(persistence, listAdapter)
      val activity = new CrudListActivity(entityType, application)
      activity.onListItemClick(null, null, -1, -1)
    }
  }
}