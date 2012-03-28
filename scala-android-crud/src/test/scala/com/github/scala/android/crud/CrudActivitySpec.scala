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
import org.mockito.Matchers._
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
    stub(application.actionsForEntity(any())).toReturn(Nil)
    val _crudType = new MyCrudType(persistence)
    val entity = Map[String,Any]("name" -> "Bob", "age" -> 25)
    val uri = UriPath(_crudType.entityName)
    val activity = new CrudActivity {
      override lazy val crudType = _crudType
      override def crudApplication = application

      override lazy val currentAction = UpdateActionName
      override def currentUriPath = uri
      override def future[T](body: => T) = new ReadyFuture[T](body)
    }
    activity.onCreate(null)
    _crudType.entityType.copy(entity, activity)
    activity.onBackPressed()
    verify(persistence).save(None, Map[String,Any]("name" -> "Bob", "age" -> 25, "uri" -> uri.toString))
    verify(persistence, never()).find(uri)
  }

  @Test
  def shouldAddIfIdNotFound() {
    stub(application.actionsForEntity(any())).toReturn(Nil)
    val _crudType = new MyCrudType(persistence)
    val entity = mutable.Map[String,Any]("name" -> "Bob", "age" -> 25)
    val uri = UriPath(_crudType.entityName)
    val activity = new CrudActivity {
      override lazy val crudType = _crudType
      override def crudApplication = application

      override lazy val currentAction = UpdateActionName
      override def currentUriPath = uri
      override def future[T](body: => T) = new ReadyFuture[T](body)
    }
    when(persistence.find(uri)).thenReturn(None)
    activity.onCreate(null)
    _crudType.entityType.copy(entity, activity)
    activity.onBackPressed()
    verify(persistence).save(None, mutable.Map[String,Any]("name" -> "Bob", "age" -> 25, "uri" -> uri.toString))
  }

  @Test
  def shouldAllowUpdating() {
    stub(application.actionsForEntity(any())).toReturn(Nil)
    val _crudType = new MyCrudType(persistence)
    val entity = mutable.Map[String,Any]("name" -> "Bob", "age" -> 25)
    val uri = UriPath(_crudType.entityName, "101")
    stub(persistence.find(uri)).toReturn(Some(entity))
    val activity = new CrudActivity {
      override lazy val crudType = _crudType
      override def crudApplication = application

      override lazy val currentAction = UpdateActionName
      override lazy val currentUriPath = uri
      override def future[T](body: => T) = new ReadyFuture[T](body)
    }
    activity.onCreate(null)
    val viewData = _crudType.entityType.copyAndTransform(activity, mutable.Map[String,Any]())
    viewData.get("name") must be (Some("Bob"))
    viewData.get("age") must be (Some(25))

    activity.onBackPressed()
    verify(persistence).save(Some(101),
      mutable.Map[String,Any]("name" -> "Bob", "age" -> 25, "uri" -> uri.toString, CursorField.idFieldName -> 101))
  }

  @Test
  def withPersistenceShouldClosePersistence() {
    val _crudType = new MyCrudType(persistence)
    val activity = new CrudActivity {
      override lazy val crudType = _crudType
      override def crudApplication = application
    }
    activity.withPersistence(p => p.findAll(UriPath.EMPTY))
    verify(persistence).close()
  }

  @Test
  def withPersistenceShouldClosePersistenceWithFailure() {
    val _crudType = new MyCrudType(persistence)
    val activity = new CrudActivity {
      override lazy val crudType = _crudType
      override def crudApplication = application
    }
    try {
      activity.withPersistence(p => throw new IllegalArgumentException("intentional"))
      fail("should have propogated exception")
    } catch {
      case e: IllegalArgumentException => "expected"
    }
    verify(persistence).close()
  }

  @Test
  def shouldHandleAnyExceptionWhenSaving() {
    stub(application.defaultContentUri).toReturn(UriPath.EMPTY)
    stub(persistence.save(None, "unsaveable data")).toThrow(new IllegalStateException("intentional"))
    val _crudType = new MyCrudType(persistence)
    val activity = new CrudActivity {
      override lazy val crudType = _crudType
      override def crudApplication = application
    }
    //should not throw an exception
    activity.saveBasedOnUserAction(persistence, "unsaveable data")
  }

  @Test
  def onPauseShouldNotCreateANewIdEveryTime() {
    stub(application.actionsForEntity(any())).toReturn(Nil)
    val _crudType = new MyCrudType(persistence)
    val entity = mutable.Map[String,Any]("name" -> "Bob", "age" -> 25)
    val uri = UriPath(_crudType.entityName)
    when(persistence.find(uri)).thenReturn(None)
    stub(persistence.save(None, mutable.Map[String,Any]("name" -> "Bob", "age" -> 25, "uri" -> uri.toString))).toReturn(101)
    val activity = new CrudActivity {
      override lazy val crudType = _crudType
      override def crudApplication = application

      override def future[T](body: => T): Future[T] = new ReadyFuture[T](body)
    }
    activity.setIntent(constructIntent(Operation.CreateActionName, uri, activity, null))
    activity.onCreate(null)
    //simulate a user entering data
    _crudType.entityType.copy(entity, activity)
    activity.onBackPressed()
    activity.onBackPressed()
    verify(persistence, times(1)).save(None, mutable.Map[String,Any]("name" -> "Bob", "age" -> 25, "uri" -> uri.toString))
    //all but the first time should provide an id
    verify(persistence).save(Some(101), mutable.Map[String,Any]("name" -> "Bob", "age" -> 25, "uri" -> (uri / 101).toString,
      CursorField.idFieldName -> 101))
  }
}