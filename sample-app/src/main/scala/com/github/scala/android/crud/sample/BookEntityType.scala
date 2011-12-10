package com.github.scala.android.crud.sample

import com.github.scala.android.crud._
import persistence.CursorField._
import persistence.EntityType
import view.ViewField._
import persistence.PersistedType._
import java.util.Date
import com.github.scala.android.crud.ParentField._

object BookEntityType extends EntityType {
  def entityName = "Book"

  def valueFields = List(
    foreignKey(AuthorEntityType),

    persisted[String]("name") + viewId(classOf[R], "name", textView),

    persistedEnum[Genre.Value]("genre", Genre) + viewId(classOf[R], "genre", enumerationView(Genre)),

    persisted[Int]("edition") + viewId(classOf[R], "edition", intView),

    persistedDate("publishDate") + viewId[Date](classOf[R], "publishDate", dateView)
  )
}

/**
 * A CRUD type for Book.
 * @book pabstec
 */

trait BookContext {
  def AuthorCrudType: CrudType

  def BookCrudType: BookCrudType

  class BookCrudType(persistenceFactory: PersistenceFactory) extends CrudType(BookEntityType, persistenceFactory) {
    def activityClass = classOf[BookActivity]
    def listActivityClass = classOf[BookListActivity]
  }
}

class BookListActivity extends CrudListActivity(SampleApplication.BookCrudType, SampleApplication)
class BookActivity extends CrudActivity(SampleApplication.BookCrudType, SampleApplication)