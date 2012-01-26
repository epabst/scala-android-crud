package com.github.scala.android.crud

import org.junit.Test
import org.junit.runner.RunWith
import com.xtremelabs.robolectric.RobolectricTestRunner
import org.scalatest.matchers.MustMatchers
import common.UriPath
import org.scalatest.mock.EasyMockSugar
import persistence.SQLiteCriteria
import ParentField.foreignKey

/** A specification for [[com.github.scala.android.crud.ParentField]].
  * @author Eric Pabst (epabst@gmail.com)
  */
@RunWith(classOf[RobolectricTestRunner])
class ParentFieldSpec extends MustMatchers with EasyMockSugar {
  @Test
  def shouldGetCriteriaCorrectlyForForeignKey() {
    val foreign = foreignKey(MyEntityType)
    val uri = UriPath(MyCrudType.entityName, "19")
    //add on extra stuff to make sure it is ignored
    val uriWithExtraStuff = uri / "foo" / 1234
    val criteria = foreign.copyAndTransform(uriWithExtraStuff, new SQLiteCriteria)
    criteria.selection must be (List(ParentField(MyEntityType).fieldName + "=19"))
  }
}