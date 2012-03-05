package com.github.scala.android.crud.sample

import com.github.scala.android.crud._
import persistence.CursorField._
import persistence.EntityType
import view.ViewField._
import com.github.triangle._
import com.github.scala.android.crud.validate.Validation._

object AuthorEntityType extends EntityType {
  def entityName = "Author"

  def valueFields = List(
    persisted[String]("name") + viewId(classOf[R], "name", textView) + requiredString,

    viewId(classOf[R], "bookCount", intView) +
            bundleField[Int]("bookCount") +
            GetterFromItem[Int] {
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
