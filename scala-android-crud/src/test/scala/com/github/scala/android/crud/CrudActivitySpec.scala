package com.github.scala.android.crud

import action.Action
import common.ReadyFuture
import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.mock.EasyMockSugar
import com.xtremelabs.robolectric.RobolectricTestRunner
import scala.collection.mutable
import org.scalatest.matchers.MustMatchers
import Action._
import android.widget.ListAdapter
import java.lang.IllegalStateException
import action.UriPath

/**
 * A test for {@link CrudListActivity}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/18/11
 * Time: 6:22 PM
 */
@RunWith(classOf[RobolectricTestRunner])
class CrudActivitySpec extends EasyMockSugar with MustMatchers with MyEntityTesting {
  @Test
  def shouldAllowAdding() {
    val persistence = mock[CrudPersistence]
    val listAdapter = mock[ListAdapter]
    val entityType = new MyEntityType(persistence, listAdapter)
    val application = mock[CrudApplication]
    val entity = mutable.Map[String,Any]("name" -> "Bob", "age" -> 25)
    val uri = UriPath(entityType.entityName)
    expecting {
      call(persistence.close())
      call(persistence.save(None, mutable.Map[String,Any]("name" -> "Bob", "age" -> 25, "uri" -> uri.toString))).andReturn(101)
      call(persistence.close())
    }
    whenExecuting(persistence, listAdapter, application) {
      val activity = new CrudActivity(entityType, application) {
        override lazy val currentAction = UpdateActionName
        override lazy val currentUriPath = uri
        override def future[T](body: => T) = new ReadyFuture[T](body)
      }
      activity.onCreate(null)
      entityType.copy(entity, activity)
      activity.onPause()
    }
  }

  @Test
  def shouldAllowUpdating() {
    val persistence = mock[CrudPersistence]
    val listAdapter = mock[ListAdapter]
    val entityType = new MyEntityType(persistence, listAdapter)
    val application = mock[CrudApplication]
    val entity = mutable.Map[String,Any]("name" -> "Bob", "age" -> 25)
    val uri = UriPath(entityType.entityName, "101")
    expecting {
      call(persistence.find(101)).andReturn(Some(entity))
      call(persistence.close())
      call(persistence.save(Some(101), mutable.Map[String,Any]("name" -> "Bob", "age" -> 25, "uri" -> uri.toString))).andReturn(101)
      call(persistence.close())
    }
    whenExecuting(persistence, listAdapter, application) {
      val activity = new CrudActivity(entityType, application) {
        override lazy val currentAction = UpdateActionName
        override lazy val currentUriPath = uri
        override def future[T](body: => T) = new ReadyFuture[T](body)
      }
      activity.onCreate(null)
      val viewData = entityType.transform(mutable.Map[String,Any](), activity)
      viewData.get("name") must be (Some("Bob"))
      viewData.get("age") must be (Some(25))

      activity.onPause()
    }
  }

  @Test
  def withPersistenceShouldClosePersistence() {
    val persistence = mock[CrudPersistence]
    val listAdapter = mock[ListAdapter]
    val application = mock[CrudApplication]
    expecting {
      call(persistence.findAll(UriPath.EMPTY)).andReturn(List[mutable.Map[String,Any]]())
      call(persistence.close())
    }
    whenExecuting(persistence, listAdapter, application) {
      val entityType = new MyEntityType(persistence, listAdapter)
      val activity = new CrudActivity(entityType, application)
      activity.withPersistence(p => p.findAll(UriPath.EMPTY))
    }
  }

  @Test
  def withPersistenceShouldClosePersistenceWithFailure() {
    val persistence = mock[CrudPersistence]
    val listAdapter = mock[ListAdapter]
    val application = mock[CrudApplication]
    expecting {
      call(persistence.close())
    }
    whenExecuting(persistence, listAdapter, application) {
      val entityType = new MyEntityType(persistence, listAdapter)
      val activity = new CrudActivity(entityType, application)
      try {
        activity.withPersistence(p => throw new IllegalArgumentException("intentional"))
        fail("should have propogated exception")
      } catch {
        case e: IllegalArgumentException => "expected"
      }
    }
  }

  @Test
  def onPauseShouldLogAnyExceptionWhenSaving() {
    val persistence = mock[CrudPersistence]
    val listAdapter = mock[ListAdapter]
    val application = mock[CrudApplication]
    expecting {
      call(persistence.save(None, "unsaveable data")).andThrow(new IllegalStateException("intentional"))
    }
    whenExecuting(persistence, listAdapter, application) {
      val entityType = new MyEntityType(persistence, listAdapter)
      val activity = new CrudActivity(entityType, application)
      //should not throw an exception
      activity.saveForOnPause(persistence, "unsaveable data")
    }
  }
}