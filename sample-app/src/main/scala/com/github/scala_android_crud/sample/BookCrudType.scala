package com.github.scala_android_crud.sample

import com.github.scala_android.crud._
import CursorField._
import ViewField._
import com.github.triangle._
import PortableField._
import PersistedType._
import ValueFormat._
import java.util.Date

/**
 * A CRUD type for Book.
 * @book pabstec
 */

trait BookContext {
  def AuthorCrudType: CrudType

  def BookCrudType: BookCrudType

  abstract class BookCrudType extends CrudType {
    def entityName = "Book"

    val authorIdField = foreignKey(AuthorCrudType)

    def fields = List(
      authorIdField,

      persisted[String]("name") + viewId(R.id.name, textView),

      persisted[Genre.Value]("genre")(enumStringType[Genre.Value](Genre)) +
            viewId(R.id.genre, enumerationSpinner(Genre)),

      persisted[Int]("edition") + viewId(R.id.edition, formatted[Int](textView)),

      persisted[Date]("publishDate") +
        viewId[Date](R.id.publishDate, datePicker + formatted(dateValueFormat, textView))
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
}

class BookListActivity extends CrudListActivity(SampleApplication.BookCrudType, SampleApplication)
class BookActivity extends CrudActivity(SampleApplication.BookCrudType, SampleApplication)