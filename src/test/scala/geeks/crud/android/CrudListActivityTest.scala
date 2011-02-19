package geeks.crud.android

import geeks.crud.EntityPersistenceComponent
import geeks.crud.EntityPersistenceComponent$EntityPersistence
import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.mock.EasyMockSugar
import com.xtremelabs.robolectric.RobolectricTestRunner
import scala.collection.mutable.Map
import CursorFieldAccess._
import android.widget.ListAdapter
import android.content.DialogInterface

//todo don't depend on futurebalance
import geeks.financial.futurebalance.android.R

/**
 * A test for {@link CrudListActivity}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/18/11
 * Time: 6:22 PM
 */
@RunWith(classOf[RobolectricTestRunner])
class CrudListActivityTest extends EasyMockSugar {
  @Test
  def shouldAllowAdding {
    val activity = new CrudListActivity[List[Map[String,Long]],Map[String,Long],Map[String,Long]]() {
      type ID = Long
      val persistence = mock[EntityPersistence]

      def fields = List(Field(persisted[Long]("age")))

      def refreshAfterSave() {}

      def listAdapter = mock[ListAdapter]

      val entityName = "MyMap"
      def listLayout: Int = 1
      def headerLayout: Int = 2
      def rowLayout: Int = 3
      def entryLayout: Int = R.layout.test_entry
      def addItemString: Int = 5
      def addDialogTitleString: Int = 6
      def editItemString: Int = 7
      def editDialogTitleString: Int = 8
      def cancelItemString: Int = 9
    }
    val entity = Map[String,Long]("age" -> 25)
    expecting {
      call(activity.persistence.newWritable).andReturn(entity)
      call(activity.persistence.save(None, entity)).andReturn(entity)
    }
    whenExecuting(activity.persistence) {
      val dialog = activity.createEditDialog(activity, None, () => {})
      dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
    }
  }
}