package geeks.crud.android

import geeks.crud.EntityPersistence
import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.mock.EasyMockSugar
import com.xtremelabs.robolectric.RobolectricTestRunner
import scala.collection.mutable.Map
import android.widget.ListAdapter
import android.content.{Intent, DialogInterface}
import geeks.crud._
import CursorFieldAccess._

//todo don't depend on futurebalance
import geeks.financial.futurebalance.android.R

/**
 * A test for {@link CrudListActivity}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/18/11
 * Time: 6:22 PM
 */
@RunWith(classOf[RobolectricTestRunner])
class CrudListActivitySpec extends EasyMockSugar {
  object MyEntityConfig extends AndroidEntityCrudConfig {
    val entityName = "MyMap"

    def fields = List(Field(persisted[Long]("age")))

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
    val activity = new CrudListActivity[Long,AnyRef,List[Map[String,Long]],Map[String,Long],Map[String,Long]](MyEntityConfig) {
      val persistence = mock[EntityPersistence[Long,AnyRef,List[Map[String,Long]],Map[String,Long],Map[String,Long]]]

      def refreshAfterSave() {}

      def listAdapter = mock[ListAdapter]
    }
    val entity = Map[String,Long]("age" -> 25)
    expecting {
      call(activity.persistence.newWritable).andReturn(entity)
      call(activity.persistence.save(None, entity)).andReturn(entity)
    }
    whenExecuting(activity.persistence) {
      activity.setIntent(new Intent(Intent.ACTION_MAIN))
      activity.onCreate(null)
      val dialog = activity.createEditDialog(activity, None)
      dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
    }
  }
}