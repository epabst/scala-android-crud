package com.github.scala.android.crud

import action.Action
import common.ReadyFuture
import org.junit.Test
import org.junit.runner.RunWith
import com.xtremelabs.robolectric.RobolectricTestRunner
import persistence.CursorField
import scala.collection.mutable
import org.scalatest.matchers.MustMatchers
import Action._
import android.widget.ListAdapter
import java.lang.IllegalStateException
import action.UriPath
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._

/**
 * A test for {@link CrudListActivity}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/18/11
 * Time: 6:22 PM
 */
@RunWith(classOf[RobolectricTestRunner])
class CrudActivitySpec extends MockitoSugar with MustMatchers with MyEntityTesting {
  val persistence = mock[CrudPersistence]
  val listAdapter = mock[ListAdapter]
  val application = mock[CrudApplication]

  @Test
  def shouldAllowAdding() {
    val entityType = new MyEntityType(persistence, listAdapter)
    val entity = mutable.Map[String,Any]("name" -> "Bob", "age" -> 25)
    val uri = UriPath(entityType.entityName)
    val activity = new CrudActivity(entityType, application) {
      override lazy val currentAction = UpdateActionName
      override def currentUriPath = uri
      override def future[T](body: => T) = new ReadyFuture[T](body)
    }
    activity.onCreate(null)
    entityType.copy(entity, activity)
    activity.onPause()
    verify(persistence).save(None, mutable.Map[String,Any]("name" -> "Bob", "age" -> 25, "uri" -> uri.toString))
  }

  @Test
  def shouldAllowUpdating() {
    val entityType = new MyEntityType(persistence, listAdapter)
    val entity = mutable.Map[String,Any]("name" -> "Bob", "age" -> 25)
    val uri = UriPath(entityType.entityName, "101")
    stub(persistence.find(101)).toReturn(Some(entity))
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
    verify(persistence).save(Some(101),
      mutable.Map[String,Any]("name" -> "Bob", "age" -> 25, "uri" -> uri.toString, CursorField.idFieldName -> 101))
  }

  @Test
  def withPersistenceShouldClosePersistence() {
    val entityType = new MyEntityType(persistence, listAdapter)
    val activity = new CrudActivity(entityType, application)
    activity.withPersistence(p => p.findAll(UriPath.EMPTY))
    verify(persistence).close()
  }

  @Test
  def withPersistenceShouldClosePersistenceWithFailure() {
    val entityType = new MyEntityType(persistence, listAdapter)
    val activity = new CrudActivity(entityType, application)
    try {
      activity.withPersistence(p => throw new IllegalArgumentException("intentional"))
      fail("should have propogated exception")
    } catch {
      case e: IllegalArgumentException => "expected"
    }
    verify(persistence).close()
  }

  @Test
  def onPauseShouldHandleAnyExceptionWhenSaving() {
    stub(persistence.save(None, "unsaveable data")).toThrow(new IllegalStateException("intentional"))
    val entityType = new MyEntityType(persistence, listAdapter)
    val activity = new CrudActivity(entityType, application)
    //should not throw an exception
    activity.saveForOnPause(persistence, "unsaveable data")
  }

  @Test
  def onPauseShouldNotCreateANewIdEveryTime() {
    val entityType = new MyEntityType(persistence, listAdapter)
    val entity = mutable.Map[String,Any]("name" -> "Bob", "age" -> 25)
    val uri = UriPath(entityType.entityName)
    stub(persistence.save(None, mutable.Map[String,Any]("name" -> "Bob", "age" -> 25, "uri" -> uri.toString))).toReturn(101)
    val activity = new CrudActivity(entityType, application) {
      override def future[T](body: => T) = new ReadyFuture[T](body)
    }
    activity.setIntent(constructIntent(Action.CreateActionName, uri, activity, null))
    activity.onCreate(null)
    //simulate a user entering data
    entityType.copy(entity, activity)
    activity.onPause()
    activity.onPause()
    verify(persistence, times(1)).save(None, mutable.Map[String,Any]("name" -> "Bob", "age" -> 25, "uri" -> uri.toString))
    //all but the first time should provide an id
    verify(persistence).save(Some(101), mutable.Map[String,Any]("name" -> "Bob", "age" -> 25, "uri" -> (uri / 101).toString,
      CursorField.idFieldName -> 101))
  }
}