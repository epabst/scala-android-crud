package com.github.scala_android.crud

import android.provider.BaseColumns
import monitor.Logging
import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.mock.EasyMockSugar
import org.easymock.EasyMock.isA
import com.xtremelabs.robolectric.RobolectricTestRunner
import org.scalatest.matchers.ShouldMatchers
import com.github.triangle._
import CursorField._
import Field._
import res.R
import android.app.Activity
import android.net.Uri
import android.content.{Intent, Context}
import android.database.{Cursor, DataSetObserver}

/**
 * A test for {@link CrudListActivity}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/18/11
 * Time: 6:22 PM
 */
@RunWith(classOf[RobolectricTestRunner])
class SQLiteCrudEntityTypeFunctionalSpec extends EasyMockSugar with ShouldMatchers with Logging {
  val runningOnRealAndroid: Boolean = try {
    debug("Seeing if running on Real Android...")
    Class.forName("com.xtremelabs.robolectric.RobolectricTestRunner")
    warn("NOT running on Real Android.")
    false
  } catch {
    case _ => {
      info("Running on Real Android.")
      true
    }
  }
  object TestEntityType extends SQLiteCrudEntityType {
    def entityName = "Person"
    val fields = List(persisted[Int]("age") + default(21))

    val listLayout = R.layout.entity_list
    val headerLayout = R.layout.test_row
    val rowLayout = R.layout.test_row
    val displayLayout = None
    val entryLayout = R.layout.test_entry
    val addItemString = R.string.add_item
    val editItemString = R.string.edit_item
    val cancelItemString = R.string.cancel_item
    def activityClass = classOf[CrudActivity[_,_,_,_]]
    def listActivityClass = classOf[CrudListActivity[_,_,_,_]]
  }

  object TestApplication extends CrudApplication {
    val name = "Test Application"

    def allEntities = List(TestEntityType)
  }

  @Test
  def shouldUseCorrectColumnNamesForFindAll() {
    val context = mock[Context]
    val crudContext = mock[CrudContext]
    expecting {
      call(crudContext.context).andReturn(context).anyTimes
      call(crudContext.application).andReturn(TestApplication).anyTimes
    }
    whenExecuting(crudContext) {
      val persistence = new SQLiteEntityPersistence(TestEntityType, crudContext)
      val result = persistence.findAll(persistence.newCriteria)
      result.getColumnIndex(BaseColumns._ID) should be (0)
      result.getColumnIndex("age") should be (1)
    }
  }

  @Test
  def shouldCloseCursorsWhenClosing() {
    val context = mock[Context]
    val crudContext = mock[CrudContext]
    expecting {
      call(crudContext.context).andReturn(context).anyTimes
      call(crudContext.application).andReturn(TestApplication).anyTimes
    }
    whenExecuting(crudContext) {
      val persistence = new SQLiteEntityPersistence(TestEntityType, crudContext)
      val writable = TestEntityType.newWritable
      TestEntityType.copyFields(Unit, writable)
      val id = persistence.save(None, writable)
      val cursors = List(
        persistence.findAll(persistence.newCriteria),
        persistence.find(id).get
      )
      persistence.close()
      for (cursor <- cursors) {
        cursor should be ('closed)
      }
    }
  }

  @Test
  def shouldRefreshCursorWhenDeletingAndSaving() {
    val activity = mock[Activity]
    val observer = mock[DataSetObserver]
    expecting {
      call(activity.getIntent).andReturn(new Intent("foo", Uri.EMPTY)).anyTimes
      call(activity.startManagingCursor(isA(classOf[Cursor]))).asStub()
      if (runningOnRealAndroid) call(observer.onChanged())
    }
    whenExecuting(activity, observer) {
      val crudContext = new CrudContext(activity, TestApplication)
      val listAdapter = TestEntityType.createListAdapter(crudContext, activity)
      listAdapter.getCount should be (0)

      val writable = TestEntityType.newWritable
      TestEntityType.copyFields(Unit, writable)
      val id = TestEntityType.withEntityPersistence(crudContext, _.save(None, writable))
      //it should have refreshed the listAdapter
      listAdapter.getCount should be (if (runningOnRealAndroid) 1 else 0)

      TestEntityType.copyFields(Map("age" -> 50), writable)
      listAdapter.registerDataSetObserver(observer)
      TestEntityType.withEntityPersistence(crudContext, _.save(Some(id), writable))
      //it should have refreshed the listAdapter (notified the observer)
      listAdapter.unregisterDataSetObserver(observer)
      listAdapter.getCount should be (if (runningOnRealAndroid) 1 else 0)

      TestEntityType.withEntityPersistence(crudContext, _.delete(List(id)))
      //it should have refreshed the listAdapter
      listAdapter.getCount should be (0)
    }
  }
}