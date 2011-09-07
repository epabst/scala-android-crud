package com.github.scala_android.crud

import android.provider.BaseColumns
import com.github.triangle.Logging
import org.junit.Test
import org.junit.runner.RunWith
import com.xtremelabs.robolectric.RobolectricTestRunner
import org.scalatest.matchers.MustMatchers
import com.github.triangle._
import persistence.CursorField._
import PortableField._
import res.R
import android.net.Uri
import android.content.{Intent, Context}
import android.database.DataSetObserver
import android.app.ListActivity
import android.widget.ListAdapter
import scala.collection._
import org.mockito.Mockito
import Mockito._
import org.mockito.Matchers._

/**
 * A test for {@link SQLiteCrudType}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/18/11
 * Time: 6:22 PM
 */
@RunWith(classOf[RobolectricTestRunner])
class SQLiteCrudTypeSpec extends MustMatchers with Logging with MyEntityTesting with CrudMockitoSugar {
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
  object TestEntityType extends SQLiteCrudType {
    def entityName = "Person"
    val valueFields = List(persisted[Int]("age") + default(21))

    val listLayout = R.layout.entity_list
    val headerLayout = R.layout.test_row
    val rowLayout = R.layout.test_row
    val displayLayout = None
    val entryLayout = R.layout.test_entry
    val addItemString = R.string.add_item
    val editItemString = R.string.edit_item
    val cancelItemString = R.string.cancel_item
    def activityClass = classOf[CrudActivity]
    def listActivityClass = classOf[CrudListActivity]
  }

  object TestApplication extends CrudApplication {
    val name = "Test Application"

    def allEntities = List(TestEntityType)
  }

  @Test
  def shouldUseCorrectColumnNamesForFindAll() {
    val context = mock[Context]
    val crudContext = mock[CrudContext]
    stub(crudContext.context).toReturn(context)
    stub(crudContext.application).toReturn(TestApplication)

    val persistence = new SQLiteEntityPersistence(TestEntityType, crudContext)
    val result = persistence.findAll(persistence.newCriteria)
    result.getColumnIndex(BaseColumns._ID) must be (0)
    result.getColumnIndex("age") must be (1)
  }

  @Test
  def shouldCloseCursorsWhenClosing() {
    val context = mock[Context]
    val crudContext = mock[CrudContext]
    stub(crudContext.context).toReturn(context)
    stub(crudContext.application).toReturn(TestApplication)

    val persistence = new SQLiteEntityPersistence(TestEntityType, crudContext)
    val writable = TestEntityType.newWritable
    TestEntityType.copy(Unit, writable)
    val id = persistence.save(None, writable)
    val cursors = List(
      persistence.findAll(persistence.newCriteria),
      persistence.find(id).get
    )
    persistence.close()
    for (cursor <- cursors) {
      cursor must be ('closed)
    }
  }

  @Test
  def shouldRefreshCursorWhenDeletingAndSaving() {
    val activity = mock[ListActivity]
    val observer = mock[DataSetObserver]
    val listAdapterCapture = capturingAnswer[Unit] { Unit }
    stub(activity.getIntent).toReturn(new Intent("foo", Uri.EMPTY))
    stub(activity.setListAdapter(anyObject())).toAnswer(listAdapterCapture)
    stub(activity.getListAdapter).toAnswer(answer {
      listAdapterCapture.params(0).asInstanceOf[ListAdapter]
    })

    val crudContext = new CrudContext(activity, TestApplication)
    TestEntityType.setListAdapter(crudContext, activity)
    val listAdapter = listAdapterCapture.params(0).asInstanceOf[ListAdapter]
    listAdapter.getCount must be (0)

    val writable = TestEntityType.newWritable
    TestEntityType.copy(Unit, writable)
    val id = TestEntityType.withEntityPersistence(crudContext, _.save(None, writable))
    //it must have refreshed the listAdapter
    listAdapter.getCount must be (if (runningOnRealAndroid) 1 else 0)

    TestEntityType.copy(Map("age" -> 50), writable)
    listAdapter.registerDataSetObserver(observer)
    TestEntityType.withEntityPersistence(crudContext, _.save(Some(id), writable))
    //it must have refreshed the listAdapter (notified the observer)
    listAdapter.unregisterDataSetObserver(observer)
    listAdapter.getCount must be (if (runningOnRealAndroid) 1 else 0)

    TestEntityType.withEntityPersistence(crudContext, _.delete(List(id)))
    //it must have refreshed the listAdapter
    listAdapter.getCount must be (0)
  }
}
