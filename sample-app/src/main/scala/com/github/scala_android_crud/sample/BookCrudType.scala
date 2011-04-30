package com.github.scala_android_crud.sample

import com.github.scala_android.crud._
import CursorFieldAccess._
import ViewFieldAccess._
import com.github.triangle._
import Field._
import PersistedType._
import ValueFormat._
import java.util.Date

/**
 * A CRUD type for Book.
 * @book pabstec
 */

object BookCrudType extends SQLiteCrudEntityType {
  def entityName = "Book"

  def fields = List(
    Field(foreignKey(AuthorCrudType)),

    Field[String](persisted("name") +
      viewId(R.id.name, textView)),

    Field[Genre.Value](persisted("genre")(enumStringType[Genre.Value](Genre)) +
          viewId(R.id.genre, enumerationSpinner(Genre))),

    Field[Int](persisted("edition") +
      viewId(R.id.edition, formatted[Int](textView))),

    Field[Date](persisted("publishDate") +
      viewId[Date](R.id.publishDate,
        datePickerFieldAccess + formatted(dateValueFormat, textView)))
  )

  def activityClass = classOf[BookActivity]

  def listActivityClass = classOf[BookListActivity]

  def entryLayout = R.layout.book_entry
  def rowLayout = R.layout.book_row
  //Use the same layout for the header
  def headerLayout = R.layout.book_row
  def listLayout = R.layout.entity_list
  def displayLayout = None

  def cancelItemString = res.R.string.cancel_item
  def editItemString = R.string.edit_book
  def addItemString = R.string.add_book
}

class BookListActivity extends CrudListActivity(BookCrudType, SampleApplication)
class BookActivity extends CrudActivity(BookCrudType, SampleApplication)