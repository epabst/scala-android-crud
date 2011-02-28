package geeks.crud.android

import _root_.android.content.{Context, Intent, DialogInterface}
import _root_.android.widget.TextView
import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.mock.EasyMockSugar
import com.xtremelabs.robolectric.RobolectricTestRunner
import scala.collection.mutable.Map
import geeks.crud._
import ViewFieldAccess._
import CursorFieldAccess._

//todo don't depend on futurebalance's R
import geeks.financial.futurebalance.android.R

/**
 * An object mother pattern for getting CrudEntityConfig instances.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/26/11
 * Time: 11:06 PM
 */

object ConfigMother extends EasyMockSugar {
  class MyEntityConfig(persistence: EntityPersistence[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]]) extends CrudEntityConfig[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]] {
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
}