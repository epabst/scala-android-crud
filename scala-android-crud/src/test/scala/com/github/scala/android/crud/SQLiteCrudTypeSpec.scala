package com.github.scala.android.crud

import action.UriPath
import android.provider.BaseColumns
import org.junit.Test
import org.junit.runner.RunWith
import com.xtremelabs.robolectric.RobolectricTestRunner
import org.scalatest.matchers.MustMatchers
import com.github.triangle._
import persistence.CursorField._
import persistence.SQLiteCriteria
import PortableField._
import res.R
import android.content.Context
import android.database.DataSetObserver
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
    def entityName = "Test"
    val valueFields = List(persisted[Int]("age") + default(21))

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
    val result = persistence.findAll(new SQLiteCriteria())
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
      persistence.findAll(new SQLiteCriteria()),
      persistence.find(id).get
    )
    persistence.close()
    for (cursor <- cursors) {
      cursor must be ('closed)
    }
  }

  @Test
  def shouldRefreshCursorWhenDeletingAndSaving() {
    val activity = mock[CrudListActivity]
    val observer = mock[DataSetObserver]
    val listAdapterCapture = capturingAnswer[Unit] { Unit }
    stub(activity.currentUriPath).toReturn(UriPath.EMPTY)
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

    TestEntityType.withEntityPersistence(crudContext, _.delete(id))
    //it must have refreshed the listAdapter
    listAdapter.getCount must be (0)
  }

  @Test
  def tableNameMustNotBeReservedWord() {
    tableNameMustNotBeReservedWord("Group")
    tableNameMustNotBeReservedWord("Table")
    tableNameMustNotBeReservedWord("index")
  }

  def tableNameMustNotBeReservedWord(name: String) {
    val crudType = new SQLiteCrudType with HiddenEntityType {
      def entityName = name

      def valueFields = List.empty[BaseField]
    }
    crudType.tableName must be (name + "0")
  }
}
