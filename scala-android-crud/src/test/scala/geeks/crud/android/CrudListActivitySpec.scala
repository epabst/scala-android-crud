package geeks.crud.android

import _root_.android.content.{Context, Intent, DialogInterface}
import _root_.android.widget.CursorAdapter
import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.mock.EasyMockSugar
import com.xtremelabs.robolectric.Robolectric
import com.xtremelabs.robolectric.RobolectricTestRunner
import scala.collection.mutable.Map
import geeks.crud._
import CursorFieldAccess._
import com.xtremelabs.robolectric.tester.android.view.TestMenu
import org.scalatest.matchers.ShouldMatchers

import com.github.scala_android.crud.R

/**
 * A test for {@link CrudListActivity}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/18/11
 * Time: 6:22 PM
 */
@RunWith(classOf[RobolectricTestRunner])
class CrudListActivitySpec extends EasyMockSugar with ShouldMatchers {
  import ConfigMother._

  @Test
  def shouldAllowAdding {
    val persistence = mock[EntityPersistence[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]]]
    val entityConfig = new MyEntityConfig(persistence)
    val activity = new CrudListActivity[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]](entityConfig) {
      def refreshAfterSave() {}
    }
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

      activity.onMenuItemSelected(0, item0) should be (true)
    }
  }

  @Test
  def shouldRefreshOnResume {
    val persistence = mock[EntityPersistence[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]]]
    val entityConfig = new MyEntityConfig(persistence)
    var refreshCount = 0
    class MyCrudListActivity extends CrudListActivity[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]](entityConfig) {
      def refreshAfterSave() {
        refreshCount = refreshCount + 1
      }

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
      refreshCount should be (0)

      activity.onResume()
      refreshCount should be (1)
    }
  }
}