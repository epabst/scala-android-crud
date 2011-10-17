package com.github.scala.android.crud.sample

import com.github.scala.android.crud._
import persistence.CursorField._
import view.ViewField._
import com.github.triangle._
import PortableField._
import GeneratedCrudType.CrudContextField

/**
 * A CRUD type for Author.
 * @author pabstec
 */
trait AuthorContext {
  def BookCrudType: CrudType

  def AuthorCrudType: AuthorCrudType

  abstract class AuthorCrudType extends CrudType {
    def entityName = "Author"

    def valueFields = List(
      persisted[String]("name") + viewId(classOf[R], "name", textView),

      viewId(classOf[R], "bookCount", intView) +
              mapField[Int]("bookCount") +
              getterFromItem[Int] {
                case PersistedId(Some(authorId)) && CrudContextField(Some(crudContext)) => {
                  println("calculating bookCount with authorId=" + authorId + " and " + crudContext)
                  BookCrudType.withEntityPersistence(crudContext, { persistence =>
                    val books = persistence.findAll(toUri(authorId))
                    Some(books.size)
                  })
                }
              }
    )

    def activityClass = classOf[AuthorActivity]
    def listActivityClass = classOf[AuthorListActivity]
  }
}

class AuthorListActivity extends CrudListActivity(SampleApplication.AuthorCrudType, SampleApplication)
class AuthorActivity extends CrudActivity(SampleApplication.AuthorCrudType, SampleApplication)
