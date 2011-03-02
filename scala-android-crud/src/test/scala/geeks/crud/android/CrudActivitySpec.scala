package geeks.crud.android

import _root_.android.content.{Context, Intent, DialogInterface}
import _root_.android.widget.{TextView, Button, CursorAdapter}
import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.mock.EasyMockSugar
import com.xtremelabs.robolectric.RobolectricTestRunner
import scala.collection.mutable.Map
import geeks.crud._
import ViewFieldAccess._
import CursorFieldAccess._
import org.scalatest.matchers.ShouldMatchers

import com.github.scala_android.crud.R

/**
 * A test for {@link CrudListActivity}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/18/11
 * Time: 6:22 PM
 */
@RunWith(classOf[RobolectricTestRunner])
class CrudActivitySpec extends EasyMockSugar with ShouldMatchers {
  import ConfigMother._

  @Test
  def shouldAllowAdding {
    val persistence = mock[EntityPersistence[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]]]
    val entityConfig = new MyEntityConfig(persistence)
    val activity = new CrudActivity[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]](entityConfig)
    val entity = Map[String,Any]("name" -> "Bob", "age" -> 25)
    val writable = Map[String,Any]("name" -> "Bob", "age" -> 25)
    expecting {
      call(persistence.newWritable).andReturn(writable)
      call(persistence.save(None, writable)).andReturn(101)
      call(persistence.close())
    }
    whenExecuting(persistence) {
      import ActivityUIActionFactory._
      activity.setIntent(constructIntent(UpdateActionString, toUri(entityConfig.entityName), activity, entityConfig.activityClass))
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
}