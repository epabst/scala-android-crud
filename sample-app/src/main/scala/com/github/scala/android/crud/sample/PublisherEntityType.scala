package com.github.scala.android.crud.sample

import com.github.scala.android.crud
import crud.CrudActivity
import crud.CrudListActivity
import crud.PersistedCrudType
import crud.PersistenceFactory
import crud.persistence.CursorField._
import crud.persistence.EntityType
import crud.view.ViewField._
import com.github.triangle._
import com.github.scala.android.crud.validate.Validation._
import PortableField._
import crud.GeneratedCrudType.{UriField, CrudContextField}

object PublisherEntityType extends EntityType {
  def entityName = "Publisher"

  def valueFields = List(
    persisted[String]("name") + viewId(classOf[R], "publisher_name", textView) + requiredString,

    viewId(classOf[R], "bookCount", intView) + bundleField[Int]("bookCount") + GetterFromItem[Int] {
      case UriField(Some(uri)) && CrudContextField(Some(crudContext)) => {
        println("calculating bookCount for " + uri + " and " + crudContext)
        crudContext.withEntityPersistence(BookEntityType) { persistence =>
          val books = persistence.findAll(uri)
          Some(books.size)
        }
      }
    }
  )
}

/** A CRUD type for Publisher.
  * @author pabstec
  */
class PublisherCrudType(persistenceFactory: PersistenceFactory) extends PersistedCrudType(PublisherEntityType, persistenceFactory) {
  def activityClass = classOf[PublisherActivity]
  def listActivityClass = classOf[PublisherListActivity]
}

class PublisherListActivity extends CrudListActivity(SampleApplication.PublisherCrudType, SampleApplication)
class PublisherActivity extends CrudActivity(SampleApplication.PublisherCrudType, SampleApplication)
