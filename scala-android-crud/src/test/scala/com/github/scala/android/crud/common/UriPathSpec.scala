package com.github.scala.android.crud.common

import org.junit.runner.RunWith
import org.junit.Test
import com.github.scala.android.crud.MyCrudType
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
  def mustFindTheIdFollowingTheEntityName() {
    val entityName = MyCrudType.entityName
    UriPath("foo").findId(entityName) must be (None)
    UriPath(entityName).findId(entityName) must be (None)
    UriPath(entityName, "123").findId(entityName) must be (Some(123))
    UriPath(entityName, "123", "foo").findId(entityName) must be (Some(123))
    UriPath(entityName, "blah").findId(entityName) must be (None)
  }
}