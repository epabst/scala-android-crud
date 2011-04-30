package com.github.scala_android.crud

import android.provider.BaseColumns
import org.junit.Test
import org.junit.runner.RunWith
import com.xtremelabs.robolectric.RobolectricTestRunner
import com.github.triangle._
import Field._
import CursorFieldAccess._
import org.scalatest.matchers.ShouldMatchers
import android.content.Intent
import android.net.Uri
import org.scalatest.mock.EasyMockSugar
import android.database.Cursor

/**
 * A specification for {@link CursorFieldAccess}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/18/11
 * Time: 6:22 PM
 */
@RunWith(classOf[RobolectricTestRunner])
class CursorFieldAccessSpec extends ShouldMatchers with EasyMockSugar {
  @Test
  def shouldGetColumnsForQueryCorrectly() {
    val foreign = foreignKey(MyCrudEntityTypeRef)
    val combined = persisted[Float]("height") + default(6.0f)
    val fields = FieldList(Field(foreign), Field(persisted[Int]("age")), Field(combined))
    val actualFields = CursorFieldAccess.queryFieldNames(fields)
    actualFields should be (List(BaseColumns._ID, foreign.fieldName, "age", "height"))
  }

  @Test
  def persistedShouldReturnNoneIfColumnNotInCursor() {
    val cursor = mock[Cursor]
    expecting {
      call(cursor.getColumnIndex("name")).andReturn(-1)
    }
    whenExecuting(cursor) {
      val field = Field[String](persisted("name"))
      field.findValue(cursor) should be (None)
    }
  }

  @Test
  def shouldGetCriteriaCorrectly() {
    val field = Field[Int](sqliteCriteria("age") + default(19))
    val criteria = new SQLiteCriteria
    field.copy(Unit, criteria)
    criteria.selection should be ("age=19")
  }

  @Test
  def shouldGetCriteriaCorrectlyForForeignKey() {
    val foreign = foreignKey(MyCrudEntityTypeRef)
    val field = Field(foreign)
    val uri = EntityUriSegment(MyCrudEntityTypeRef.entityName, "19").specifyInUri(Uri.EMPTY)
    //add on extra stuff to make sure it is ignored
    val intent = new Intent("", Uri.withAppendedPath(uri, "foo/1234"))
    val criteria = new SQLiteCriteria
    field.copy(intent, criteria)
    criteria.selection should be (foreign.fieldName + "=19")
  }
}