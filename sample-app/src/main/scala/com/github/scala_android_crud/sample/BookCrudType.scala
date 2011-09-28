package com.github.scala_android_crud.sample

import com.github.scala_android.crud._
import persistence.CursorField._
import view.ViewField._
import persistence.PersistedType._
import java.util.Date
import com.github.scala_android.crud.ParentField._

/**
 * A CRUD type for Book.
 * @book pabstec
 */

trait BookContext {
  def AuthorCrudType: CrudType

  def BookCrudType: BookCrudType

  abstract class BookCrudType extends CrudType {
    def entityName = "Book"

    def valueFields = List(
      foreignKey(AuthorCrudType),

      persisted[String]("name") + viewId(R.id.name, textView),

      persisted[Genre.Value]("genre")(enumStringType[Genre.Value](Genre)) +
            viewId(R.id.genre, enumerationView(Genre)),

      persisted[Int]("edition") + viewId(R.id.edition, intView),

      persisted[Date]("publishDate") +
        viewId[Date](R.id.publishDate, dateView)
    )

    def activityClass = classOf[BookActivity]
    def listActivityClass = classOf[BookListActivity]

    def cancelItemString = res.R.string.cancel_item
    def editItemString = R.string.edit_book
    def addItemString = R.string.add_book
  }
}

class BookListActivity extends CrudListActivity(SampleApplication.BookCrudType, SampleApplication)
class BookActivity extends CrudActivity(SampleApplication.BookCrudType, SampleApplication)