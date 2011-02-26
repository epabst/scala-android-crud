package geeks.crud.android

import _root_.android.content.{Context, Intent, DialogInterface}
import _root_.android.net.Uri
import _root_.android.widget.CursorAdapter
import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.mock.EasyMockSugar
import com.xtremelabs.robolectric.RobolectricTestRunner
import scala.collection.mutable.Map
import geeks.crud._
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
class ActivityUIActionFactorySpec extends EasyMockSugar with ShouldMatchers {
  //todo determine if shadowing, and run tests on real Android device as well.
  val isShadowing = true

  object MyEntityType extends CrudEntityType {
    def entityName = "MyEntity"

    def listActivityClass = classOf[CrudListActivity[_,_,_,_]]

    def addItemString = R.string.add_item

    def editItemString = R.string.edit_item

    def cancelItemString = R.string.cancel_item

    def activityClass = classOf[CrudActivity[_,_,_,_]]
  }

  import ActivityUIActionFactory._
  import MyEntityType.entityName

  val context = null

  @Test
  def getCreateIntentShouldGetTheRightUri {
    val intent = new Intent(null, toUri("foo"))
    //sanity check
    intent.getData should be (toUri("foo"))
    getCreateIntent(MyEntityType, new Intent(null, toUri("foo")), context).getData should
      be (toUri("foo", entityName))
    getCreateIntent(MyEntityType, new Intent(null, toUri("foo", entityName)), context).getData should
      be (toUri("foo", entityName))
    getCreateIntent(MyEntityType, new Intent(null, toUri("foo", entityName, "123")), context).getData should
      be (toUri("foo", entityName))
    getCreateIntent(MyEntityType, new Intent(null, toUri("foo", entityName, "123", "bar")), context).getData should
      be (toUri("foo", entityName))
    getCreateIntent(MyEntityType, new Intent(null, toUri()), context).getData should
      be (toUri(entityName))
  }

  @Test
  def shouldGetTheRightAction {
    if (!isShadowing) {
      getCreateIntent(MyEntityType, new Intent(null, toUri("foo")), context).getAction should be (Intent.ACTION_INSERT)
      getDisplayListIntent(MyEntityType, Unit, new Intent(null, toUri("foo")), context).getAction should be (Intent.ACTION_PICK)
      getDisplayIntent(MyEntityType, 45, new Intent(null, toUri("foo", entityName)), context).getAction should be (Intent.ACTION_VIEW)
      getUpdateIntent(MyEntityType, 45, new Intent(null, toUri("foo", entityName)), context).getAction should be (Intent.ACTION_EDIT)
      getDeleteIntent(MyEntityType, List(45), new Intent(null, toUri("foo", entityName)), context).getAction should be (Intent.ACTION_DELETE)
    }
  }
}