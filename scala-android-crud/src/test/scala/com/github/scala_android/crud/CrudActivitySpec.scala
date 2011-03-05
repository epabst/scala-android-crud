package com.github.scala_android.crud

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.mock.EasyMockSugar
import com.xtremelabs.robolectric.RobolectricTestRunner
import scala.collection.mutable.Map
import org.scalatest.matchers.ShouldMatchers

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
    import ActivityUIActionFactory._
    val persistence = mock[EntityPersistence[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]]]
    val entityConfig = new MyEntityConfig(persistence)
    val activity = new CrudActivity[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]](entityConfig)
    val entity = Map[String,Any]("name" -> "Bob", "age" -> 25)
    val writable = Map[String,Any]()
    val uri = toUri(entityConfig.entityName)
    expecting {
      call(persistence.close())
      call(persistence.newWritable).andReturn(writable)
      call(persistence.save(None, writable)).andAnswer(answer {
        writable.get("name") should be (Some("Bob"))
        writable.get("age") should be (Some(25))
        writable.get("uri") should be (Some(uri.toString))
        101
      })
      call(persistence.close())
    }
    whenExecuting(persistence) {
      activity.setIntent(constructIntent(UpdateActionString, uri, activity, entityConfig.activityClass))
      activity.onCreate(null)
      val viewData = Map[String,Any]()
      entityConfig.copyFields(entity, activity)
      activity.onStop()
    }
  }

  @Test
  def shouldAllowUpdating {
    val persistence = mock[EntityPersistence[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]]]
    val entityConfig = new MyEntityConfig(persistence)
    val activity = new CrudActivity[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]](entityConfig)
    val entity = Map[String,Any]("name" -> "Bob", "age" -> 25)
    val writable = Map[String,Any]()
    expecting {
      call(persistence.find(101)).andReturn(entity)
      call(persistence.close())
      call(persistence.newWritable).andReturn(writable)
      call(persistence.save(Some(101), writable)).andReturn(101)
      call(persistence.close())
    }
    whenExecuting(persistence) {
      import ActivityUIActionFactory._
      activity.setIntent(constructIntent(UpdateActionString, toUri(entityConfig.entityName, "101"), activity, entityConfig.activityClass))
      activity.onCreate(null)
      val viewData = Map[String,Any]()
      entityConfig.copyFields(activity, viewData)
      viewData.get("name") should be (Some("Bob"))
      viewData.get("age") should be (Some(25))

      activity.onStop()
    }
  }

  @Test
  def withPersistenceShouldClosePersistence {
    val persistence = mock[EntityPersistence[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]]]
    val entityConfig = new MyEntityConfig(persistence)
    val activity = new CrudActivity[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]](entityConfig)
    expecting {
      call(persistence.findAll(Unit)).andReturn(List[Map[String,Any]]())
      call(persistence.close)
    }
    whenExecuting(persistence) {
      activity.withPersistence(p => p.findAll(Unit))
    }
  }

  @Test
  def withPersistenceShouldClosePersistenceWithFailure {
    val persistence = mock[EntityPersistence[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]]]
    val entityConfig = new MyEntityConfig(persistence)
    val activity = new CrudActivity[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]](entityConfig)
    expecting {
      call(persistence.close)
    }
    whenExecuting(persistence) {
      try {
        activity.withPersistence(p => throw new IllegalArgumentException("intentional"))
        fail("should have propogated exception")
      } catch {
        case e: IllegalArgumentException => "expected"
      }
    }
  }
}