package com.github.scala_android.crud

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.mock.EasyMockSugar
import com.xtremelabs.robolectric.RobolectricTestRunner
import scala.collection.mutable.Map
import org.scalatest.matchers.ShouldMatchers
import ActivityUIActionFactory._
import android.widget.ListAdapter

/**
 * A test for {@link CrudListActivity}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/18/11
 * Time: 6:22 PM
 */
@RunWith(classOf[RobolectricTestRunner])
class CrudActivitySpec extends EasyMockSugar with ShouldMatchers with MyEntityTesting {
  @Test
  def shouldAllowAdding {
    val persistence = mock[CrudEntityPersistence[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]]]
    val listAdapter = mock[ListAdapter]
    val entityType = new MyEntityType(persistence, listAdapter)
    val application = mock[CrudApplication]
    val entity = Map[String,Any]("name" -> "Bob", "age" -> 25)
    val uri = toUri(entityType.entityName)
    expecting {
      call(persistence.close())
      call(persistence.save(None, Map[String,Any]("name" -> "Bob", "age" -> 25, "uri" -> uri.toString))).andReturn(101)
      call(persistence.close())
    }
    whenExecuting(persistence, listAdapter, application) {
      val activity = new CrudActivity[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]](entityType, application)
      activity.setIntent(constructIntent(UpdateActionString, uri, activity, entityType.activityClass))
      activity.onCreate(null)
      val viewData = Map[String,Any]()
      entityType.copyFields(entity, activity)
      activity.onPause()
    }
  }

  @Test
  def shouldAllowUpdating {
    val persistence = mock[CrudEntityPersistence[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]]]
    val listAdapter = mock[ListAdapter]
    val entityType = new MyEntityType(persistence, listAdapter)
    val application = mock[CrudApplication]
    val entity = Map[String,Any]("name" -> "Bob", "age" -> 25)
    val uri = toUri(entityType.entityName, "101")
    expecting {
      call(persistence.find(101)).andReturn(Some(entity))
      call(persistence.close())
      call(persistence.save(Some(101), Map[String,Any]("name" -> "Bob", "age" -> 25, "uri" -> uri.toString))).andReturn(101)
      call(persistence.close())
    }
    whenExecuting(persistence, listAdapter, application) {
      import ActivityUIActionFactory._
      val activity = new CrudActivity[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]](entityType, application)
      activity.setIntent(constructIntent(UpdateActionString, uri, activity, entityType.activityClass))
      activity.onCreate(null)
      val viewData = Map[String,Any]()
      entityType.copyFields(activity, viewData)
      viewData.get("name") should be (Some("Bob"))
      viewData.get("age") should be (Some(25))

      activity.onPause()
    }
  }

  @Test
  def withPersistenceShouldClosePersistence {
    val persistence = mock[CrudEntityPersistence[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]]]
    val listAdapter = mock[ListAdapter]
    val application = mock[CrudApplication]
    expecting {
      call(persistence.findAll(Unit)).andReturn(List[Map[String,Any]]())
      call(persistence.close)
    }
    whenExecuting(persistence, listAdapter, application) {
      val entityType = new MyEntityType(persistence, listAdapter)
      val activity = new CrudActivity[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]](entityType, application)
      activity.withPersistence(p => p.findAll(Unit))
    }
  }

  @Test
  def withPersistenceShouldClosePersistenceWithFailure {
    val persistence = mock[CrudEntityPersistence[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]]]
    val listAdapter = mock[ListAdapter]
    val application = mock[CrudApplication]
    expecting {
      call(persistence.close)
    }
    whenExecuting(persistence, listAdapter, application) {
      val entityType = new MyEntityType(persistence, listAdapter)
      val activity = new CrudActivity[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]](entityType, application)
      try {
        activity.withPersistence(p => throw new IllegalArgumentException("intentional"))
        fail("should have propogated exception")
      } catch {
        case e: IllegalArgumentException => "expected"
      }
    }
  }
}