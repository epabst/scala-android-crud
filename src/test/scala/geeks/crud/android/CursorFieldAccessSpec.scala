package geeks.crud.android

import android.provider.BaseColumns
import geeks.crud.EntityPersistenceComponent$EntityPersistence
import org.junit.Test
import org.junit.runner.RunWith
import com.xtremelabs.robolectric.RobolectricTestRunner
import geeks.crud._
import CursorFieldAccess._
import org.scalatest.matchers.ShouldMatchers
//todo don't depend on futurebalance
/**
 * A specification for {@link CursorFieldAccess}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/18/11
 * Time: 6:22 PM
 */
@RunWith(classOf[RobolectricTestRunner])
class CursorFieldAccessSpec extends ShouldMatchers {
  @Test
  def shouldGetColumnsForQueryCorrectly {
    val fields = List(Field(persisted[Long]("age")))
    val actualFields = CursorFieldAccess.queryFieldNames(fields)
    actualFields should be (List(BaseColumns._ID, "age"))
  }
}