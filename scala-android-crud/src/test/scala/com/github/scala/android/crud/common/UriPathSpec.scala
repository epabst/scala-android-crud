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
  val entityName = MyCrudType.entityName

  @Test
  def mustFindTheIdFollowingTheEntityName() {
    UriPath("foo").findId(entityName) must be (None)
    UriPath(entityName).findId(entityName) must be (None)
    UriPath(entityName, "123").findId(entityName) must be (Some(123))
    UriPath(entityName, "123", "foo").findId(entityName) must be (Some(123))
    UriPath(entityName, "blah").findId(entityName) must be (None)
  }

  @Test
  def upToIdOfMustStripOfWhateverIsAfterTheID() {
    val uri = UriPath("abc", "123", entityName, "456", "def")
    uri.upToIdOf(entityName) must be (UriPath("abc", "123", entityName, "456"))
  }

  @Test
  def upToIdOfMustNotFailIfNoIDFoundButAlsoPreserveWhatIsThereAlready() {
    val uri = UriPath("abc", "123", "def")
    uri.upToIdOf(entityName).segments.startsWith(uri.segments) must be (true)
  }

  @Test
  def mustConvertFromString() {
    val uriPath = UriPath("abc", "123", "def")
    UriPath(uriPath.toString) must be (uriPath)
  }

  @Test
  def mustConvertFromEmptyUri() {
    UriPath(UriPath.EMPTY.toString) must be(UriPath.EMPTY)
  }

  @Test
  def mustConvertFromEmptyString() {
    UriPath("") must be (UriPath.EMPTY)
  }
}