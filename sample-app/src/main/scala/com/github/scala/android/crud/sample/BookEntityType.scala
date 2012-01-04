package com.github.scala.android.crud.sample

import com.github.scala.android.crud._
import persistence.CursorField._
import persistence.EntityType
import view.ViewField._
import persistence.PersistedType._
import java.util.Date
import com.github.scala.android.crud.ParentField._
import view.{EntityView, EnumerationView}

object BookEntityType extends EntityType {
  def entityName = "Book"

  def valueFields = List(
    foreignKey(AuthorEntityType),

    foreignKey(PublisherEntityType) + EntityView(PublisherEntityType),

    persisted[String]("name") + viewId(classOf[R], "name", textView),

    persistedEnum[Genre.Value]("genre", Genre) + viewId(classOf[R], "genre", EnumerationView[Genre.Value](Genre)),

    persisted[Int]("edition") + viewId(classOf[R], "edition", intView),

    persistedDate("publishDate") + viewId[Date](classOf[R], "publishDate", dateView)
  )
}

/** A CRUD type for Book.
  * @author pabstec
  */
class BookCrudType(persistenceFactory: PersistenceFactory) extends CrudType(BookEntityType, persistenceFactory) {
  def activityClass = classOf[BookActivity]
  def listActivityClass = classOf[BookListActivity]
}

class BookListActivity extends CrudListActivity(SampleApplication.BookCrudType, SampleApplication)
class BookActivity extends CrudActivity(SampleApplication.BookCrudType, SampleApplication)
