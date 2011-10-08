package com.github.scala.android.crud.sample

import com.github.scala.android.crud._
import persistence.CursorField._
import view.ViewField._
import persistence.PersistedType._
import java.util.Date
import com.github.scala.android.crud.ParentField._

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

      persisted[String]("name") + viewId(classOf[R], "name", textView),

      persisted[Genre.Value]("genre")(enumStringType[Genre.Value](Genre)) +
            viewId(classOf[R], "genre", enumerationView(Genre)),

      persisted[Int]("edition") + viewId(classOf[R], "edition", intView),

      persisted[Date]("publishDate") +
        viewId[Date](classOf[R], "publishDate", dateView)
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