package com.github.scala.android.crud

import action.Operation
import common.ReadyFuture
import org.junit.Test
import org.junit.runner.RunWith
import com.xtremelabs.robolectric.RobolectricTestRunner
import persistence.CursorField
import scala.collection.mutable
import org.scalatest.matchers.MustMatchers
import Operation._
import android.widget.ListAdapter
import java.lang.IllegalStateException
import common.UriPath
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import actors.Future

/** A test for [[com.github.scala.android.crud.CrudListActivity]].
  * @author Eric Pabst (epabst@gmail.com)
  */
@RunWith(classOf[RobolectricTestRunner])
class CrudActivitySpec extends MockitoSugar with MustMatchers {
  val persistenceFactory = mock[PersistenceFactory]
  val persistence = mock[CrudPersistence]
  val listAdapter = mock[ListAdapter]
  val application = mock[CrudApplication]

  @Test
  def shouldSupportAddingWithoutEverFinding() {
    val crudType = new MyCrudType(persistence)
    val entity = Map[String,Any]("name" -> "Bob", "age" -> 25)
    val uri = UriPath(crudType.entityName)
    val activity = new CrudActivity(crudType, application) {
      override lazy val currentAction = UpdateActionName
      override def currentUriPath = uri
      override def future[T](body: => T) = new ReadyFuture[T](body)
    }
    activity.onCreate(null)
    crudType.entityType.copy(entity, activity)
    activity.onPause()
    verify(persistence).save(None, Map[String,Any]("name" -> "Bob", "age" -> 25, "uri" -> uri.toString))
    verify(persistence, never()).find(uri)
  }

  @Test
  def shouldAddIfIdNotFound() {
    val crudType = new MyCrudType(persistence)
    val entity = mutable.Map[String,Any]("name" -> "Bob", "age" -> 25)
    val uri = UriPath(crudType.entityName)
    val activity = new CrudActivity(crudType, application) {
      override lazy val currentAction = UpdateActionName
      override def currentUriPath = uri
      override def future[T](body: => T) = new ReadyFuture[T](body)
    }
    when(persistence.find(uri)).thenReturn(None)
    activity.onCreate(null)
    crudType.entityType.copy(entity, activity)
    activity.onPause()
    verify(persistence).save(None, mutable.Map[String,Any]("name" -> "Bob", "age" -> 25, "uri" -> uri.toString))
  }

  @Test
  def shouldAllowUpdating() {
    val crudType = new MyCrudType(persistence)
    val entity = mutable.Map[String,Any]("name" -> "Bob", "age" -> 25)
    val uri = UriPath(crudType.entityName, "101")
    stub(persistence.find(uri)).toReturn(Some(entity))
    val activity = new CrudActivity(crudType, application) {
      override lazy val currentAction = UpdateActionName
      override lazy val currentUriPath = uri
      override def future[T](body: => T) = new ReadyFuture[T](body)
    }
    activity.onCreate(null)
    val viewData = crudType.entityType.copyAndTransform(activity, mutable.Map[String,Any]())
    viewData.get("name") must be (Some("Bob"))
    viewData.get("age") must be (Some(25))

    activity.onPause()
    verify(persistence).save(Some(101),
      mutable.Map[String,Any]("name" -> "Bob", "age" -> 25, "uri" -> uri.toString, CursorField.idFieldName -> 101))
  }

  @Test
  def withPersistenceShouldClosePersistence() {
    val crudType = new MyCrudType(persistence)
    val activity = new CrudActivity(crudType, application)
    activity.withPersistence(p => p.findAll(UriPath.EMPTY))
    verify(persistence).close()
  }

  @Test
  def withPersistenceShouldClosePersistenceWithFailure() {
    val crudType = new MyCrudType(persistence)
    val activity = new CrudActivity(crudType, application)
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
    val crudType = new MyCrudType(persistence)
    val activity = new CrudActivity(crudType, application)
    //should not throw an exception
    activity.saveForOnPause(persistence, "unsaveable data")
  }

  @Test
  def onPauseShouldNotCreateANewIdEveryTime() {
    val crudType = new MyCrudType(persistence)
    val entity = mutable.Map[String,Any]("name" -> "Bob", "age" -> 25)
    val uri = UriPath(crudType.entityName)
    when(persistence.find(uri)).thenReturn(None)
    stub(persistence.save(None, mutable.Map[String,Any]("name" -> "Bob", "age" -> 25, "uri" -> uri.toString))).toReturn(101)
    val activity = new CrudActivity(crudType, application) {
      override def future[T](body: => T): Future[T] = new ReadyFuture[T](body)
    }
    activity.setIntent(constructIntent(Operation.CreateActionName, uri, activity, null))
    activity.onCreate(null)
    //simulate a user entering data
    crudType.entityType.copy(entity, activity)
    activity.onPause()
    activity.onPause()
    verify(persistence, times(1)).save(None, mutable.Map[String,Any]("name" -> "Bob", "age" -> 25, "uri" -> uri.toString))
    //all but the first time should provide an id
    verify(persistence).save(Some(101), mutable.Map[String,Any]("name" -> "Bob", "age" -> 25, "uri" -> (uri / 101).toString,
      CursorField.idFieldName -> 101))
  }
}