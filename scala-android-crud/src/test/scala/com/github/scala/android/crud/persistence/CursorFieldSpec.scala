package com.github.scala.android.crud.persistence

import android.provider.BaseColumns
import org.junit.Test
import org.junit.runner.RunWith
import com.xtremelabs.robolectric.RobolectricTestRunner
import com.github.triangle._
import PortableField._
import CursorField._
import org.scalatest.matchers.MustMatchers
import org.scalatest.mock.EasyMockSugar
import android.database.Cursor
import com.github.scala.android.crud.common.PlatformTypes._

/**
 * A specification for {@link CursorField}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/18/11
 * Time: 6:22 PM
 */
@RunWith(classOf[RobolectricTestRunner])
class CursorFieldSpec extends MustMatchers with EasyMockSugar {
  @Test
  def shouldGetColumnsForQueryCorrectly() {
    val foreign = persisted[ID]("foreignID")
    val combined = persisted[Float]("height") + default(6.0f)
    val fields = FieldList(CursorField.PersistedId, foreign, persisted[Int]("age"), combined)
    val actualFields = CursorField.queryFieldNames(fields)
    actualFields must be (List(BaseColumns._ID, "foreignID", "age", "height"))
  }

  @Test
  def persistedShouldReturnNoneIfColumnNotInCursor() {
    val cursor = mock[Cursor]
    expecting {
      call(cursor.getColumnIndex("name")).andReturn(-1)
    }
    whenExecuting(cursor) {
      val field = persisted[String]("name")
      field.getter(cursor) must be (None)
    }
  }

  @Test
  def shouldGetCriteriaCorrectlyForANumber() {
    val field = sqliteCriteria[Int]("age") + default(19)
    val criteria: SQLiteCriteria = field.transform(new SQLiteCriteria, PortableField.UseDefaults)
    criteria.selection must be (List("age=19"))
  }

  @Test
  def shouldGetCriteriaCorrectlyForAString() {
    val field = sqliteCriteria[String]("name") + default("John Doe")
    val criteria: SQLiteCriteria = field.transform(new SQLiteCriteria, PortableField.UseDefaults)
    criteria.selection must be (List("name=\"John Doe\""))
  }

  @Test
  def shouldHandleMultipleSelectionCriteria() {
    val field = sqliteCriteria[Int]("age") + sqliteCriteria[Int]("alternateAge") + default(19)
    val criteria: SQLiteCriteria = field.transform(new SQLiteCriteria, PortableField.UseDefaults)
    criteria.selection.sorted must be (List("age=19", "alternateAge=19"))
  }
}