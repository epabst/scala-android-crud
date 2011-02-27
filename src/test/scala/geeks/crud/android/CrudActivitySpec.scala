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

//todo don't depend on futurebalance's R
import geeks.financial.futurebalance.android.R

/**
 * A test for {@link CrudListActivity}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/18/11
 * Time: 6:22 PM
 */
@RunWith(classOf[RobolectricTestRunner])
class CrudActivitySpec extends EasyMockSugar with ShouldMatchers {
  val persistence = mock[EntityPersistence[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]]]
  object MyEntityConfig extends CrudEntityConfig[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]] {
    val entityName = "MyMap"

    def fields = List(
      Field(persisted[String]("name"), viewId[TextView,String](R.id.name)),
      Field(persisted[Long]("age"), viewId[TextView,Long](R.id.age)))

    def getEntityPersistence(context: Context) = persistence

    val listLayout = R.layout.entity_list
    val headerLayout = R.layout.test_row
    val rowLayout = R.layout.test_row
    val entryLayout = R.layout.test_entry
    val addItemString = R.string.add_item
    val editItemString = R.string.edit_item
    val cancelItemString = R.string.cancel_item

    def listActivityClass = classOf[CrudListActivity[_,_,_,_]]
    def activityClass = classOf[CrudActivity[_,_,_,_]]
  }

  @Test
  def shouldAllowUpdating {
    val activity = new CrudActivity[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]](MyEntityConfig)
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
      activity.setIntent(constructIntent(UpdateActionString, toUri(MyEntityConfig.entityName, "101"), activity, MyEntityConfig.activityClass))
      activity.onCreate(null)
      val viewData = Map[String,Any]()
      MyEntityConfig.copyFields(activity, viewData)
      viewData.get("name") should be (Some("Bob"))
      viewData.get("age") should be (Some(25))

      activity.onStop()
    }
  }
}