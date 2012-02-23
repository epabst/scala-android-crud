package com.github.scala.android.crud

import _root_.android.content.Intent
import org.junit.Test
import org.junit.runner.RunWith
import com.xtremelabs.robolectric.RobolectricTestRunner
import com.xtremelabs.robolectric.tester.android.view.TestMenu
import org.scalatest.matchers.MustMatchers
import android.view.{View, ContextMenu}
import org.mockito.Mockito._
import org.mockito.Matchers._
import persistence.EntityType
import android.os.Bundle
import java.util.ArrayList
import res.R
import android.widget.TextView
import android.util.SparseArray
import view.{CacheValue, AdapterCaching}
import java.util.concurrent.CountDownLatch

/** A test for [[com.github.scala.android.crud.CrudListActivity]].
  * @author Eric Pabst (epabst@gmail.com)
  */
@RunWith(classOf[RobolectricTestRunner])
class CrudListActivitySpec extends MustMatchers with CrudMockitoSugar {
  @Test
  def mustNotCopyFromParentEntityIfUriPathIsInsufficient() {
    val crudType = mock[CrudType]
    val parentCrudType = mock[CrudType]
    val application = mock[CrudApplication]
    val entityType = mock[EntityType]
    stub(crudType.entityType).toReturn(entityType)
    stub(crudType.parentEntities(application)).toReturn(List(parentCrudType))
    stub(crudType.maySpecifyEntityInstance(any())).toReturn(false)

    val activity = new CrudListActivity(crudType, application)
    activity.populateFromParentEntities()
    verify(crudType, never()).copyFromPersistedEntity(any(), any())
  }

  @Test
  def shouldAllowAdding() {
    val application = mock[CrudApplication]
    val persistence = mock[CrudPersistence]
    val entityType = new MyEntityType
    val crudType = new MyCrudType(entityType, persistence)
    stub(application.crudType(entityType)).toReturn(crudType)
    when(persistence.findAll(any())).thenReturn(Seq(Map[String,Any]("name" -> "Bob", "age" -> 25)))
    val activity = new CrudListActivity(crudType, application)
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
    val application = mock[CrudApplication]
    val contextMenu = mock[ContextMenu]
    val ignoredView: View = null
    val ignoredMenuInfo: ContextMenu.ContextMenuInfo = null
    stub(application.allCrudTypes).toReturn(Nil)
    val crudType = MyCrudType
    val activity = new CrudListActivity(crudType, application)
    activity.onCreateContextMenu(contextMenu, ignoredView, ignoredMenuInfo)
    verify(contextMenu).add(0, res.R.string.delete_item, 0, res.R.string.delete_item)
  }

  @Test
  def shouldHandleNoEntityOptions() {
    val application = mock[CrudApplication]
    val contextMenu = mock[ContextMenu]
    val ignoredView: View = null
    val ignoredMenuInfo: ContextMenu.ContextMenuInfo = null

    val crudType = new MyCrudType(new MyEntityType) {
      override def getEntityActions(application: CrudApplication) = Nil
    }
    val activity = new CrudListActivity(crudType, application)
    //shouldn't do anything
    activity.onCreateContextMenu(contextMenu, ignoredView, ignoredMenuInfo)
  }

  lazy val sparseArrayWorking: Boolean = {
    val array = new SparseArray[String]()
    array.put(0, "hello")
    val working = array.get(0) == "hello"
    if (working) error("SparseArray is now working!  You must have upgraded to a robolectric version that supports it.  Delete this code.")
    working
  }

  @Test
  def mustSaveInstanceState() {
    val persistenceFactory = mock[PersistenceFactory]
    val persistence = mock[CrudPersistence]
    stub(persistenceFactory.createEntityPersistence(anyObject(), anyObject())).toReturn(persistence)
    val map = Map[String, Any]("name" -> "Bob", "age" -> 25)
    when(persistence.findAll(any())).thenReturn(Seq(map))
    val application = mock[CrudApplication]
    val entityType = new MyEntityType
    val crudType = new MyCrudType(entityType, persistenceFactory)
    stub(application.crudType(entityType)).toReturn(crudType)
    class MyCrudListActivity extends CrudListActivity(crudType, application) {
      //make it public for testing
      override def onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
      }
    }
    val activity = new MyCrudListActivity
    activity.setIntent(new Intent(Intent.ACTION_MAIN))
    activity.onCreate(null)
    val Some(actor) = AdapterCaching.findCacheActor(activity.getListView)
    val latch = new CountDownLatch(1)
    actor ! CacheValue(0, entityType.copyFrom(map), () => latch.countDown())
    latch.await()
    val state = new Bundle()
    activity.onSaveInstanceState(state)
    val bundleList = state.getSparseParcelableArray[Bundle](entityType.entityName)
    if (!sparseArrayWorking) return
    bundleList.size() must be (1)
    val bundle0 = bundleList.get(0)
    bundle0.getString("name") must be ("Bob")
    bundle0.getInt("age") must be (25)
  }

  @Test
  def mustRestoreInstanceState() {
    val persistenceFactory = mock[PersistenceFactory]
    val persistence = mock[CrudPersistence]
    stub(persistenceFactory.createEntityPersistence(anyObject(), anyObject())).toReturn(persistence)
    when(persistence.findAll(any())).thenReturn(Seq(Map[String,Any]("name" -> "Bob", "age" -> 25)))
    val application = mock[CrudApplication]
    val entityType = new MyEntityType
    val crudType = new MyCrudType(entityType, persistenceFactory)
    stub(application.crudType(entityType)).toReturn(crudType)
    class MyCrudListActivity extends CrudListActivity(crudType, application) {
      //make it public for testing
      override def onRestoreInstanceState(state: Bundle) {
        super.onRestoreInstanceState(state)
      }
    }

    val state = new Bundle()
    val list = new SparseArray[Bundle]()
    val bundle0 = new Bundle()
    bundle0.putString("name", "Robert")
    bundle0.putInt("age", 40)
    list.put(0, bundle0)
    state.putSparseParcelableArray(entityType.entityName, list)

    val activity = new MyCrudListActivity
    activity.setIntent(new Intent(Intent.ACTION_MAIN))
    activity.onCreate(null)
    if (!sparseArrayWorking) return
    activity.onRestoreInstanceState(state)
    activity.findViewById(R.id.name).asInstanceOf[TextView].getText must be ("Robert")
    activity.findViewById(R.id.age).asInstanceOf[TextView].getText must be ("40")
  }

  @Test
  def createMustRestoreInstanceState() {
    val persistenceFactory = mock[PersistenceFactory]
    val persistence = mock[CrudPersistence]
    stub(persistenceFactory.createEntityPersistence(anyObject(), anyObject())).toReturn(persistence)
    when(persistence.findAll(any())).thenReturn(Seq(Map[String,Any]("name" -> "Bob", "age" -> 25)))
    val application = mock[CrudApplication]
    val entityType = new MyEntityType
    val crudType = new MyCrudType(entityType, persistenceFactory)
    stub(application.crudType(entityType)).toReturn(crudType)
    class MyCrudListActivity extends CrudListActivity(crudType, application) {
      //make it public for testing
      override def onRestoreInstanceState(state: Bundle) {
        super.onRestoreInstanceState(state)
      }
    }

    val state = new Bundle()
    val list = new ArrayList[Bundle]()
    val bundle0 = new Bundle()
    bundle0.putString("name", "Robert")
    bundle0.putInt("age", 40)
    list.add(bundle0)
    state.putParcelableArrayList(entityType.entityName, list)

    val activity = new MyCrudListActivity
    activity.setIntent(new Intent(Intent.ACTION_MAIN))
    activity.onCreate(state)
    if (!sparseArrayWorking) return
    activity.findViewById(R.id.name).asInstanceOf[TextView].getText must be ("Robert")
    activity.findViewById(R.id.age).asInstanceOf[TextView].getText must be ("40")
  }

  @Test
  def shouldRefreshOnResume() {
    val persistenceFactory = mock[PersistenceFactory]
    val persistence = mock[CrudPersistence]
    stub(persistenceFactory.createEntityPersistence(anyObject(), anyObject())).toReturn(persistence)
    when(persistence.findAll(any())).thenReturn(Seq(Map[String,Any]("name" -> "Bob", "age" -> 25)))
    val application = mock[CrudApplication]
    val entityType = new MyEntityType
    val crudType = new MyCrudType(entityType, persistenceFactory)
    stub(application.crudType(entityType)).toReturn(crudType)
    class MyCrudListActivity extends CrudListActivity(crudType, application) {
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
    //verify(persistenceFactory, never()).refreshAfterDataChanged(anyObject())

    activity.onResume()
    //verify(persistenceFactory, times(1)).refreshAfterDataChanged(anyObject())
  }

  @Test
  def shouldIgnoreClicksOnHeader() {
    val application = mock[CrudApplication]
    val crudType = MyCrudType
    val activity = new CrudListActivity(crudType, application)
    // should do nothing
    activity.onListItemClick(null, null, -1, -1)
  }
}