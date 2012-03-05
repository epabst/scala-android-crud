package com.github.scala.android.crud.common

import org.junit.runner.RunWith
import com.github.scala.android.crud.MyCrudType
import org.scalatest.matchers.MustMatchers
import org.scalatest.junit.JUnitRunner
import org.scalatest.Spec

/** A behavior specification for [[com.github.scala.android.crud.common.UriPath]].
  * @author Eric Pabst (epabst@gmail.com)
  */
@RunWith(classOf[JUnitRunner])
class UriPathSpec extends Spec with MustMatchers {
  val entityName = MyCrudType.entityName

  describe("findId") {
    it("must find the id following the entity name") {
      UriPath("foo").findId(entityName) must be (None)
      UriPath(entityName).findId(entityName) must be (None)
      UriPath(entityName, "123").findId(entityName) must be (Some(123))
      UriPath(entityName, "123", "foo").findId(entityName) must be (Some(123))
      UriPath(entityName, "blah").findId(entityName) must be (None)
    }
  }

  describe("lastEntityNameOption") {
    it("must handle an empty UriPath") {
      UriPath.EMPTY.lastEntityNameOption must be (None)
    }

    it("must get the last entityName") {
      UriPath("a", "b", "c").lastEntityNameOption must be (Some("c"))
    }

    it("must get the last entityName even if followed by an ID") {
      UriPath("a", "1").lastEntityNameOption must be (Some("a"))
      UriPath("a", "1", "b", "c", "3").lastEntityNameOption must be (Some("c"))
    }
  }

  describe("upToIdOf") {
    it("must strip of whatever is after the ID") {
      val uri = UriPath("abc", "123", entityName, "456", "def")
      uri.upToIdOf(entityName) must be (UriPath("abc", "123", entityName, "456"))
    }

    it("must not fail if no ID found but also preserve what is there already") {
      val uri = UriPath("abc", "123", "def")
      uri.upToIdOf(entityName).segments.startsWith(uri.segments) must be (true)
    }
  }

  it("must convert from a string") {
    val uriPath = UriPath("abc", "123", "def")
    UriPath(uriPath.toString) must be (uriPath)
  }

  it("must convert from an empty uri") {
    UriPath(UriPath.EMPTY.toString) must be(UriPath.EMPTY)
  }

  it ("must convert from an empty string") {
    UriPath("") must be (UriPath.EMPTY)
  }
}
