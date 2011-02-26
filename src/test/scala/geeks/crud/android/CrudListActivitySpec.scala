package geeks.crud.android

import _root_.android.content.{Context, Intent, DialogInterface}
import _root_.android.widget.CursorAdapter
import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.mock.EasyMockSugar
import com.xtremelabs.robolectric.RobolectricTestRunner
import scala.collection.mutable.Map
import geeks.crud._
import CursorFieldAccess._

//todo don't depend on futurebalance's R
import geeks.financial.futurebalance.android.R

/**
 * A test for {@link CrudListActivity}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/18/11
 * Time: 6:22 PM
 */
@RunWith(classOf[RobolectricTestRunner])
class CrudListActivitySpec extends EasyMockSugar {
  val persistence = mock[EntityPersistence[AnyRef,List[Map[String,Long]],Map[String,Long],Map[String,Long]]]
  object MyEntityConfig extends CrudEntityConfig[AnyRef,List[Map[String,Long]],Map[String,Long],Map[String,Long]] {
    val entityName = "MyMap"

    def fields = List(Field(persisted[Long]("age")))

    def getEntityPersistence(context: Context) = persistence

    val listLayout = R.layout.entity_list
    val headerLayout = R.layout.test_row
    val rowLayout = R.layout.test_row
    val entryLayout = R.layout.test_entry
    val addItemString = R.string.add_item
    val editItemString = R.string.edit_item
    val cancelItemString = R.string.cancel_item
  }

  @Test
  def shouldAllowAdding {
    val activity = new CrudListActivity[AnyRef,List[Map[String,Long]],Map[String,Long],Map[String,Long]](MyEntityConfig) {
      def refreshAfterSave() {}
    }
    val entity = Map[String,Long]("age" -> 25)
    val listAdapter = mock[CursorAdapter]
    expecting {
      call(persistence.createListAdapter(activity)).andReturn(listAdapter)
      call(persistence.newWritable).andReturn(entity)
      call(persistence.save(None, entity)).andReturn(88)
    }
    whenExecuting(persistence, listAdapter) {
      activity.setIntent(new Intent(Intent.ACTION_MAIN))
      activity.onCreate(null)
      val dialog = activity.createEditDialog(activity, None)
      dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
    }
  }
}