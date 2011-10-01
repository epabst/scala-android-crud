package com.github.scala.android.crud.sample

import com.github.scala.android.crud._
import persistence.CursorField._
import view.ViewField._
import com.github.triangle._
import PortableField._
import GeneratedCrudType.crudContextField

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
      persisted[String]("name") + viewId(R.id.name, textView),

      viewId(R.id.bookCount, intView) +
              mapField[Int]("bookCount") +
              new FieldTuple2(persistedId, crudContextField) with CalculatedField[Int] {
                def calculate = { case Values(Some(authorId), Some(crudContext)) =>
                  println("calculating bookCount with authorId=" + authorId + " and " + crudContext)
                  BookCrudType.withEntityPersistence(crudContext, { persistence =>
                    val books = persistence.findAll(toUri(authorId))
                    Some(books.size)
                  })
                }
              }
    )

    def cancelItemString = res.R.string.cancel_item
    def editItemString = R.string.edit_author
    def addItemString = R.string.add_author

    def activityClass = classOf[AuthorActivity]
    def listActivityClass = classOf[AuthorListActivity]
  }
}

class AuthorListActivity extends CrudListActivity(SampleApplication.AuthorCrudType, SampleApplication)
class AuthorActivity extends CrudActivity(SampleApplication.AuthorCrudType, SampleApplication)
