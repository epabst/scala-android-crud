package com.github.scala_android.crud

import org.junit.runner.RunWith
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.EasyMockSugar
import com.xtremelabs.robolectric.RobolectricTestRunner
import org.junit.Test
import android.os.Bundle


/**
 * A behavior specification for {@link ViewFieldAccess}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/9/11
 * Time: 7:59 PM
 */

@RunWith(classOf[RobolectricTestRunner])
class PersistedTypeSpec extends ShouldMatchers with EasyMockSugar {
  @Test
  def itShouldReadAndWriteBundle() {
    import PersistedType._
    verifyPersistedTypeWithBundle("hello")
    verifyPersistedTypeWithBundle(100L)
  }

  def verifyPersistedTypeWithBundle[T](value: T)(implicit persistedType: PersistedType[T]) {
    val bundle = new Bundle()
    persistedType.putValue(bundle, "foo", value)
    persistedType.getValue(bundle, "foo") should be (Some(value))
  }

  @Test
  def itShouldGiveCorrectSQLiteType() {
    import PersistedType._
    stringType.sqliteType should be ("TEXT")
    blobType.sqliteType should be ("BLOB")
    longRefType.sqliteType should be ("INTEGER")
    longType.sqliteType should be ("INTEGER")
    intRefType.sqliteType should be ("INTEGER")
    intType.sqliteType should be ("INTEGER")
    shortRefType.sqliteType should be ("INTEGER")
    shortType.sqliteType should be ("INTEGER")
    byteRefType.sqliteType should be ("INTEGER")
    byteType.sqliteType should be ("INTEGER")
    doubleRefType.sqliteType should be ("REAL")
    doubleType.sqliteType should be ("REAL")
    floatRefType.sqliteType should be ("REAL")
    floatType.sqliteType should be ("REAL")
  }
}