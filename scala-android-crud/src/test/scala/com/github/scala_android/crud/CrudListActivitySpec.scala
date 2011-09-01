package com.github.scala_android.crud

import _root_.android.content.Intent
import android.widget.ListAdapter
import org.junit.Test
import org.junit.runner.RunWith
import com.xtremelabs.robolectric.RobolectricTestRunner
import persistence.CrudPersistence
import com.xtremelabs.robolectric.tester.android.view.TestMenu
import org.scalatest.matchers.MustMatchers
import android.view.{View, ContextMenu}
import org.mockito.Mockito._

/**
 * A test for {@link CrudListActivity}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/18/11
 * Time: 6:22 PM
 */
@RunWith(classOf[RobolectricTestRunner])
class CrudListActivitySpec extends MustMatchers with MyEntityTesting with CrudMockitoSugar {
  @Test
  def shouldAllowAdding() {
    val persistence = mock[CrudPersistence]
    val application = mock[CrudApplication]
    val listAdapter = mock[ListAdapter]
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

  @Test
  def shouldHaveCorrectContextMenu() {
    val persistence = mock[CrudPersistence]
    val application = mock[CrudApplication]
    val contextMenu = mock[ContextMenu]
    val listAdapter = mock[ListAdapter]
    val ignoredView: View = null
    val ignoredMenuInfo: ContextMenu.ContextMenuInfo = null
    stub(application.allEntities).toReturn(Nil)
    val entityType = new MyEntityType(persistence, listAdapter)
    val activity = new CrudListActivity(entityType, application)
    activity.onCreateContextMenu(contextMenu, ignoredView, ignoredMenuInfo)
    verify(contextMenu).add(0, 0, 0, res.R.string.delete_item)
  }

  @Test
  def shouldHandleNoEntityOptions() {
    val persistence = mock[CrudPersistence]
    val application = mock[CrudApplication]
    val contextMenu = mock[ContextMenu]
    val listAdapter = mock[ListAdapter]
    val ignoredView: View = null
    val ignoredMenuInfo: ContextMenu.ContextMenuInfo = null

    val entityType = new MyEntityType(persistence, listAdapter) {
      override def getEntityActions(application: CrudApplication) = Nil
    }
    val activity = new CrudListActivity(entityType, application)
    //shouldn't do anything
    activity.onCreateContextMenu(contextMenu, ignoredView, ignoredMenuInfo)
  }

  @Test
  def shouldRefreshOnResume() {
    val persistence = mock[CrudPersistence]
    val application = mock[CrudApplication]
    val listAdapter = mock[ListAdapter]
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

  @Test
  def shouldIgnoreClicksOnHeader() {
    val persistence = mock[CrudPersistence]
    val application = mock[CrudApplication]
    val listAdapter = mock[ListAdapter]
    val entityType = new MyEntityType(persistence, listAdapter)
    val activity = new CrudListActivity(entityType, application)
    // should do nothing
    activity.onListItemClick(null, null, -1, -1)
  }
}