package com.github.scala_android.crud

import action.UriPath
import org.junit.Test
import org.junit.runner.RunWith
import com.xtremelabs.robolectric.RobolectricTestRunner
import org.scalatest.matchers.MustMatchers
import android.content.Intent
import android.net.Uri
import org.scalatest.mock.EasyMockSugar
import persistence.SQLiteCriteria
import ParentField.foreignKey

/**
 * A specification for {@link ParentField}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/18/11
 * Time: 6:22 PM
 */
@RunWith(classOf[RobolectricTestRunner])
class ParentFieldSpec extends MustMatchers with EasyMockSugar {
  @Test
  def shouldGetCriteriaCorrectlyForForeignKey() {
    val foreign = foreignKey(MyCrudType)
    val uri = UriPath(MyCrudType.entityName, "19").specifyInUri(Uri.EMPTY)
    //add on extra stuff to make sure it is ignored
    val intent = new Intent("", Uri.withAppendedPath(uri, "foo/1234"))
    val criteria = new SQLiteCriteria
    foreign.copy(intent, criteria)
    criteria.selection must be (ParentField(MyCrudType).fieldName + "=19")
  }
}