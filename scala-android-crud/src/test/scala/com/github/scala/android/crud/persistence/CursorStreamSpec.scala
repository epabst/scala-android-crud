package com.github.scala.android.crud.persistence

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.Spec
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import android.database.Cursor

/** A behavior specification for [[com.github.scala.android.crud.persistence.EntityPersistence]].
  * @author Eric Pabst (epabst@gmail.com)
  */
@RunWith(classOf[JUnitRunner])
class CursorStreamSpec extends Spec with MustMatchers with MockitoSugar {
  it("must handle an empty Cursor") {
    val field = CursorField.persisted[String]("name")
    val cursor = mock[Cursor]
    stub(cursor.moveToNext()).toReturn(false)
    val stream = CursorStream(cursor, List(field))
    stream.isEmpty must be (true)
    stream.size must be (0)
  }

  it("must not instantiate the entire Stream for an infinite Cursor") {
    val field = CursorField.persisted[String]("name")

    val cursor = mock[Cursor]
    stub(cursor.moveToNext).toReturn(true)
    stub(cursor.getColumnIndex("name")).toReturn(1)
    stub(cursor.getString(1)).toReturn("Bryce")

    val stream = CursorStream(cursor, List(field))
    val second = stream.tail.head
    field(second) must be ("Bryce")
  }

  it("must have correct size") {
    val field = CursorField.persisted[String]("name")

    val cursor = mock[Cursor]
    when(cursor.moveToNext).thenReturn(true).thenReturn(true).thenReturn(false)
    stub(cursor.getColumnIndex("name")).toReturn(1)
    stub(cursor.getString(1)).toReturn("Allen")

    val stream = CursorStream(cursor, List(field))
    stream.size must be (2)
  }

  it("must allow accessing data from different positions in any order") {
    val field = CursorField.persisted[String]("name")

    val cursor = mock[Cursor]
    when(cursor.moveToNext).thenReturn(true).thenReturn(true).thenReturn(false)
    stub(cursor.getColumnIndex("name")).toReturn(1)
    when(cursor.getString(1)).thenReturn("Allen").thenReturn("Bryce")

    val stream = CursorStream(cursor, List(field))
    val second = stream.tail.head
    val first = stream.head
    field(second) must be ("Bryce")
    field(first) must be ("Allen")
  }
}
