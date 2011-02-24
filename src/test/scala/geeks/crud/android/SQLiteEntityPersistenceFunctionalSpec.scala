package geeks.crud.android

import android.provider.BaseColumns
import geeks.crud.EntityPersistence
import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.mock.EasyMockSugar
import com.xtremelabs.robolectric.RobolectricTestRunner
import scala.collection.mutable.Map
import android.widget.ListAdapter
import org.scalatest.matchers.ShouldMatchers
import android.content.{Context, DialogInterface}
import android.database.Cursor
import android.database.sqlite.{SQLiteDatabase, SQLiteOpenHelper}
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
class SQLiteEntityPersistenceFunctionalSpec extends EasyMockSugar with ShouldMatchers {
  object TestEntityConfig extends AndroidEntityCrudConfig {
    def entityName = "Person"
    val fields = List(Field(persisted[Long]("age")))

    val listLayout = R.layout.entity_list
    val headerLayout = R.layout.test_row
    val rowLayout = R.layout.test_row
    val entryLayout = R.layout.test_entry
    val addItemString = R.string.add_item
    val editItemString = R.string.edit_item
    val cancelItemString = R.string.cancel_item
  }

  @Test
  def shouldUseCorrectColumnNamesForFindAll {
    val mockContext = mock[Context]
    val component = new SQLiteEntityPersistenceComponent with TestingDatabaseComponent {
      val persistence = new SQLiteEntityPersistence(TestEntityConfig)
      def context = mockContext
    }
    expecting {
    }
    whenExecuting(mockContext) {
      val result = component.persistence.findAll(new SQLiteCriteria())
      result.getColumnIndex(BaseColumns._ID) should be (0)
      result.getColumnIndex("age") should be (1)
    }
  }
}