package com.github.scala_android.crud

import _root_.android.content.Intent
import _root_.android.widget.CursorAdapter
import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.mock.EasyMockSugar
import com.xtremelabs.robolectric.RobolectricTestRunner
import scala.collection.mutable.Map
import com.xtremelabs.robolectric.tester.android.view.TestMenu
import org.scalatest.matchers.ShouldMatchers
import android.view.{MenuItem, View, ContextMenu}

/**
 * A test for {@link CrudListActivity}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/18/11
 * Time: 6:22 PM
 */
@RunWith(classOf[RobolectricTestRunner])
class CrudListActivitySpec extends EasyMockSugar with ShouldMatchers with MyEntityTesting {
  @Test
  def shouldAllowAdding {
    val persistence = mock[EntityPersistence[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]]]
    val entityType = new MyEntityType(persistence)
    val activity = new CrudListActivity[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]](entityType)
    val entity = Map[String,Any]("age" -> 25)
    val listAdapter = mock[CursorAdapter]
    expecting {
      call(persistence.createListAdapter(activity)).andReturn(listAdapter)
    }
    whenExecuting(persistence, listAdapter) {
      activity.setIntent(new Intent(Intent.ACTION_MAIN))
      activity.onCreate(null)
      val menu = new TestMenu(activity)
      activity.onCreateOptionsMenu(menu)
      val item0 = menu.getItem(0)
      val drawable = item0.getIcon
      item0.getTitle.toString should be ("Add")
      menu.size should be (1)

      activity.onOptionsItemSelected(item0) should be (true)
    }
  }

  @Test
  def shouldHaveCorrectContextMenu {
    val persistence = mock[EntityPersistence[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]]]
    val entityType = new MyEntityType(persistence)
    val activity = new CrudListActivity[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]](entityType)
    val contextMenu = mock[ContextMenu]
    val menuItem = mock[MenuItem]
    val ignoredView: View = null
    val ignoredMenuInfo: ContextMenu.ContextMenuInfo = null
    expecting {
      call(contextMenu.add(0, 0, 0, res.R.string.delete_item)).andReturn(menuItem)
    }
    whenExecuting(contextMenu) {
      activity.onCreateContextMenu(contextMenu, ignoredView, ignoredMenuInfo)
    }
  }

  @Test
  def shouldHandleNoEntityOptions {
    val persistence = mock[EntityPersistence[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]]]
    val entityType = new MyEntityType(persistence) {
      override def getEntityActions(actionFactory: UIActionFactory) = Nil
    }
    val activity = new CrudListActivity[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]](entityType)
    val contextMenu = mock[ContextMenu]
    val ignoredView: View = null
    val ignoredMenuInfo: ContextMenu.ContextMenuInfo = null
    whenExecuting(contextMenu) {
      //shouldn't do anything
      activity.onCreateContextMenu(contextMenu, ignoredView, ignoredMenuInfo)
    }
  }

  @Test
  def shouldRefreshOnResume {
    val persistence = mock[EntityPersistence[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]]]
    val entityType = new MyEntityType(persistence)
    var refreshCount = 0
    class MyCrudListActivity extends CrudListActivity[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]](entityType) {
      //make it public for testing
      override def onPause() {
        super.onPause
      }

      //make it public for testing
      override def onResume() {
        super.onResume
      }
    }
    val activity = new MyCrudListActivity
    val entity = Map[String,Any]("age" -> 25)
    val listAdapter = mock[CursorAdapter]
    expecting {
      call(persistence.createListAdapter(activity)).andReturn(listAdapter)
    }
    whenExecuting(persistence, listAdapter) {
      activity.setIntent(new Intent(Intent.ACTION_MAIN))
      activity.onCreate(null)
      activity.onPause()
      entityType.refreshCount should be (0)

      activity.onResume()
      entityType.refreshCount should be (1)
    }
  }

  @Test
  def shouldIgnoreClicksOnHeader {
    val persistence = mock[EntityPersistence[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]]]
    val entityType = new MyEntityType(persistence)
    val activity = new CrudListActivity[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]](entityType)
    activity.onListItemClick(null, null, -1, -1)
  }
}