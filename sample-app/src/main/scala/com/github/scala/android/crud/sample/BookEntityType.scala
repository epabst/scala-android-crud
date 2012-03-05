package com.github.scala.android.crud.sample

import com.github.scala.android.crud._
import persistence.CursorField._
import persistence.EntityType
import persistence.PersistedType._
import java.util.Date
import com.github.scala.android.crud.ParentField._
import view.ViewField._
import view.{EntityView, EnumerationView}
import com.github.scala.android.crud.validate.Validation._

object BookEntityType extends EntityType {
  def entityName = "Book"

  def valueFields = List(
    foreignKey(AuthorEntityType),

    persisted[String]("name") + viewId(classOf[R], "name", textView) + requiredString,

    persisted[Int]("edition") + viewId(classOf[R], "edition", intView),

    persistedEnum[Genre.Value]("genre", Genre) + viewId(classOf[R], "genre", EnumerationView[Genre.Value](Genre)),

    foreignKey(PublisherEntityType) + viewId(classOf[R], "publisher", EntityView(PublisherEntityType)),

    persistedDate("publishDate") + viewId[Date](classOf[R], "publishDate", dateView)
  )
}
