package com.github.scala_android.crud.action

import org.junit.runner.RunWith
import org.junit.Test
import com.github.scala_android.crud.action.Action._
import com.github.scala_android.crud.MyCrudType
import org.scalatest.matchers.MustMatchers
import com.xtremelabs.robolectric.RobolectricTestRunner

/**
 * A behavior specification for {@link UriPath}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 9/22/11
 * Time: 10:40 PM
 */
@RunWith(classOf[RobolectricTestRunner])
class UriPathSpec extends MustMatchers {

  @Test
  def segmentShouldFindId() {
    val entityName = MyCrudType.entityName
    UriPath(entityName).findId(toUri("foo")) must be (None)
    UriPath(entityName).findId(toUri(entityName)) must be (None)
    UriPath(entityName).findId(toUri(entityName, "123")) must be (Some(123))
    UriPath(entityName).findId(toUri(entityName, "123", "foo")) must be (Some(123))
    UriPath(entityName).findId(toUri(entityName, "blah")) must be (None)
  }
}